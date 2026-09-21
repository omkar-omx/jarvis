package com.jarvis.app.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.jarvis.app.JarvisApplication
import com.jarvis.app.accessibility.AccessibilityBridge
import kotlinx.coroutines.delay

/**
 * Autonomous WhatsApp Navigation & Intelligent Contact / Nickname Resolver for J.A.R.V.I.S.
 *
 * Supports Hindi, Hinglish, and English voice/text commands. Resolves nicknames
 * (e.g., "mere dost", "bhai", "my friend", "papa") by querying persistent Room memory,
 * and navigates WhatsApp UI hands-free via Accessibility or native intents.
 */
object WhatsAppNavigator {

    private const val TAG = "WhatsAppNavigator"

    data class WhatsAppTask(
        val rawRecipient: String,
        val message: String
    )

    data class ResolvedTarget(
        val displayName: String,
        val phoneNumber: String? = null,
        val isResolvedFromMemory: Boolean = false,
        val isUnknownRelation: Boolean = false
    )

    /**
     * Checks if a user command targets WhatsApp messaging.
     */
    fun isWhatsAppMessagingCommand(command: String): Boolean {
        val lower = command.lowercase().trim()
        val hasWa = lower.contains("whatsapp") || lower.contains("whats app")
        val hasMsg = lower.contains("message") || lower.contains("msg") || lower.contains("mssg") ||
                lower.contains("bhejo") || lower.contains("bhej") || lower.contains("send") ||
                lower.contains("bol") || lower.contains("bolo")
        return hasWa && hasMsg
    }

    /**
     * Parses the raw command into recipient and message payload.
     */
    fun parseTask(command: String): WhatsAppTask {
        val cleanCmd = command.trim()

        // 1. "whatsapp pe {target} ko {msg} message karo / bhejo / bol"
        val regex1 = Regex("""whatsapp (?:pe|par|me)?\s*(.*?)\s+ko\s+(?:message|msg|mssg|bol|bolo)?\s*(?:karo|kar|bhejo|bhej)?\s*(.*)""", RegexOption.IGNORE_CASE)
        val match1 = regex1.find(cleanCmd)
        if (match1 != null) {
            val rec = match1.groupValues[1].trim()
            var msg = match1.groupValues[2].trim()
            msg = cleanMessageLead(msg)
            if (rec.isNotBlank()) return WhatsAppTask(rec, msg)
        }

        // 2. "{target} ko whatsapp pe message bhejo {msg}"
        val regex2 = Regex("""(.*?)\s+ko\s+whatsapp\s+(?:pe|par|me)?\s+(?:message|msg|mssg|bol|bolo)?\s*(?:karo|kar|bhejo|bhej)?\s*(.*)""", RegexOption.IGNORE_CASE)
        val match2 = regex2.find(cleanCmd)
        if (match2 != null) {
            val rec = match2.groupValues[1].trim()
            var msg = match2.groupValues[2].trim()
            msg = cleanMessageLead(msg)
            if (rec.isNotBlank()) return WhatsAppTask(rec, msg)
        }

        // 3. English: "send message to {target} on whatsapp saying {msg}"
        val regex3 = Regex("""send (?:a )?(?:whatsapp )?message to (.*?) on whatsapp (?:saying|that)?\s*(.*)""", RegexOption.IGNORE_CASE)
        val match3 = regex3.find(cleanCmd)
        if (match3 != null) {
            return WhatsAppTask(match3.groupValues[1].trim(), cleanMessageLead(match3.groupValues[2].trim()))
        }

        // 4. "whatsapp message {target} {msg}"
        val regex4 = Regex("""whatsapp (?:message|msg)\s+(?:to\s+)?(.*?)\s+(.*)""", RegexOption.IGNORE_CASE)
        val match4 = regex4.find(cleanCmd)
        if (match4 != null) {
            return WhatsAppTask(match4.groupValues[1].trim(), cleanMessageLead(match4.groupValues[2].trim()))
        }

        // 5. Fallback extraction
        val fallbackTarget = cleanCmd
            .replace("whatsapp", "", ignoreCase = true)
            .replace("pe", "", ignoreCase = true)
            .replace("ko", "", ignoreCase = true)
            .replace("message", "", ignoreCase = true)
            .replace("karo", "", ignoreCase = true)
            .trim()
        return WhatsAppTask(fallbackTarget, "")
    }

    private fun cleanMessageLead(msg: String): String {
        return msg
            .removePrefix("message ")
            .removePrefix("msg ")
            .removePrefix("mssg ")
            .removePrefix("ki ")
            .removePrefix("saying ")
            .removePrefix("that ")
            .removePrefix("bhejo ")
            .removePrefix("kar ")
            .removePrefix("karo ")
            .trim()
    }

    /**
     * Resolves nicknames or contact references from Room memory vault.
     */
    suspend fun resolveTarget(raw: String): ResolvedTarget {
        val lowerRaw = raw.lowercase().trim()
        val relationshipKeywords = listOf("dost", "friend", "bhai", "brother", "papa", "father", "mom", "mother", "mummy", "behen", "sister", "wife")
        val isRelation = relationshipKeywords.any { lowerRaw.contains(it) }

        val cleanRelationKey = lowerRaw
            .removePrefix("mere ")
            .removePrefix("mera ")
            .removePrefix("meri ")
            .removePrefix("my ")
            .trim()

        val memories = try {
            JarvisApplication.memoryRepository.getAllMemories()
        } catch (_: Exception) {
            emptyList()
        }

        if (isRelation) {
            // Search memory for relationship link
            for (mem in memories) {
                val content = mem.content
                val lowerContent = content.lowercase()

                val matchesRelation = lowerContent.contains(cleanRelationKey) ||
                        (cleanRelationKey.contains("dost") && lowerContent.contains("friend")) ||
                        (cleanRelationKey.contains("friend") && lowerContent.contains("dost"))

                if (matchesRelation) {
                    val phoneRegex = Regex("""(\+?\d[\d -]{8,14}\d)""")
                    val phoneMatch = phoneRegex.find(content)
                    val phone = phoneMatch?.value?.replace(" ", "")?.replace("-", "")

                    // Look for name in memory: "naam [Name] hai" or "is [Name]" or "[Relation] [Name]"
                    val nameMatch = Regex("""(?:naam|name)\s+(?:hai\s+)?([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE).find(content)
                        ?: Regex("""(?:naam|name)\s+is\s+([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE).find(content)
                        ?: Regex("""(?:dost|friend|bhai|brother|papa|mom)\s+(?:hai\s+|is\s+)?([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE).find(content)
                        ?: Regex("""([A-Za-z0-9]+)\s+(?:is my|mera|mere)\s+(?:dost|friend|bhai|brother)""", RegexOption.IGNORE_CASE).find(content)

                    val resolvedName = nameMatch?.groupValues?.get(1)?.trim()
                        ?.removeSuffix("hai")
                        ?.removeSuffix("is")
                        ?.trim()

                    if (!resolvedName.isNullOrBlank()) {
                        return ResolvedTarget(
                            displayName = resolvedName.replaceFirstChar { it.uppercase() },
                            phoneNumber = phone,
                            isResolvedFromMemory = true
                        )
                    } else if (phone != null) {
                        return ResolvedTarget(
                            displayName = raw,
                            phoneNumber = phone,
                            isResolvedFromMemory = true
                        )
                    }
                }
            }

            // Relationship was requested, but no matching memory was stored
            return ResolvedTarget(
                displayName = raw,
                isUnknownRelation = true
            )
        }

        // Direct name: check if a phone number is registered for this name in memory
        for (mem in memories) {
            val content = mem.content
            if (content.contains(lowerRaw, ignoreCase = true)) {
                val phoneRegex = Regex("""(\+?\d[\d -]{8,14}\d)""")
                val phoneMatch = phoneRegex.find(content)
                val phone = phoneMatch?.value?.replace(" ", "")?.replace("-", "")
                if (phone != null) {
                    return ResolvedTarget(
                        displayName = raw.replaceFirstChar { it.uppercase() },
                        phoneNumber = phone,
                        isResolvedFromMemory = true
                    )
                }
            }
        }

        return ResolvedTarget(
            displayName = raw.replaceFirstChar { it.uppercase() },
            phoneNumber = null,
            isResolvedFromMemory = false
        )
    }

    /**
     * Executes the WhatsApp messaging action via Intent or UI Accessibility automation.
     */
    suspend fun dispatch(command: String, context: Context): DispatchResult {
        val task = parseTask(command)
        val resolved = resolveTarget(task.rawRecipient)

        if (resolved.isUnknownRelation) {
            return DispatchResult(
                handled = true,
                feedback = "I couldn't find a record for '${task.rawRecipient}' in the Neural Vault, sir. Tell me: 'Yaad rakhna mere dost ka naam [Name] hai' to link this nickname.",
                success = false
            )
        }

        val contactName = resolved.displayName
        val phone = resolved.phoneNumber
        val messageText = task.message.ifBlank { "Hello" }

        // Tier 1: Direct Deep-Link if phone number is known
        if (!phone.isNullOrBlank()) {
            try {
                val cleanPhone = phone.replace("+", "").replace("-", "").replace(" ", "").trim()
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(messageText)}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)

                if (AccessibilityBridge.isConnected.value) {
                    delay(1200)
                    clickSendButton()
                    return DispatchResult(
                        handled = true,
                        feedback = "Opened WhatsApp chat with $contactName ($cleanPhone) and autonomously dispatched message: \"$messageText\", sir.",
                        success = true
                    )
                } else {
                    return DispatchResult(
                        handled = true,
                        feedback = "Opened WhatsApp chat with $contactName ($cleanPhone) with pre-filled message: \"$messageText\", sir.",
                        success = true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch WhatsApp phone deep-link", e)
            }
        }

        // Tier 2: Accessibility UI Navigation (Autonomous Search & Send)
        if (AccessibilityBridge.isConnected.value) {
            val navSuccess = executeAccessibilityNavigation(context, contactName, messageText)
            if (navSuccess) {
                val memoryNote = if (resolved.isResolvedFromMemory) " (resolved from Neural Vault as '${task.rawRecipient}')" else ""
                return DispatchResult(
                    handled = true,
                    feedback = "Successfully navigated WhatsApp, located $contactName$memoryNote, and sent: \"$messageText\", sir.",
                    success = true
                )
            }
        }

        // Tier 3: Native WhatsApp Share Intent (Zero Accessibility required)
        val fallbackSuccess = launchWhatsAppShareIntent(context, messageText)
        val memoryNote = if (resolved.isResolvedFromMemory) " for $contactName (resolved from '${task.rawRecipient}')" else " for $contactName"
        return if (fallbackSuccess) {
            DispatchResult(
                handled = true,
                feedback = "Opening WhatsApp$memoryNote with your message: \"$messageText\". (💡 Tip: Enable Accessibility Automation in Protocols for 100% hands-free auto-navigation and sending, sir).",
                success = true
            )
        } else {
            DispatchResult(
                handled = true,
                feedback = "Could not launch WhatsApp. Please check if WhatsApp is installed, sir.",
                success = false
            )
        }
    }

    private suspend fun executeAccessibilityNavigation(context: Context, contactName: String, message: String): Boolean {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp") ?: return false
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)

            // 1. Wait for WhatsApp home screen
            delay(1000)
            var screen = AccessibilityBridge.readCurrentScreen() ?: return false

            // 2. Locate and tap Search button
            val searchNode = screen.findNodeByText("Search")
                ?: screen.findClickableElements().firstOrNull {
                    it.viewIdResourceName?.contains("menuitem_search") == true ||
                            it.contentDescription?.contains("Search", ignoreCase = true) == true ||
                            it.contentDescription?.contains("खोजें", ignoreCase = true) == true
                }

            if (searchNode != null) {
                AccessibilityBridge.performTap(searchNode.id)
                delay(600)
            }

            // 3. Type contact name into search input field
            screen = AccessibilityBridge.readCurrentScreen() ?: return false
            val searchInput = screen.findEditableElements().firstOrNull()
                ?: screen.nodes.firstOrNull { it.viewIdResourceName?.contains("search_src_text") == true }

            if (searchInput != null) {
                AccessibilityBridge.performTypeText(contactName, searchInput.id)
            } else {
                AccessibilityBridge.performTypeText(contactName)
            }

            // 4. Wait for search results
            delay(1200)
            screen = AccessibilityBridge.readCurrentScreen() ?: return false

            // 5. Find matching contact in results list (excluding the search input itself)
            val contactNode = screen.nodes.flatMap { it.findClickable() }.firstOrNull { node ->
                !node.isEditable && (node.text?.contains(contactName, ignoreCase = true) == true ||
                        node.contentDescription?.contains(contactName, ignoreCase = true) == true)
            } ?: screen.findNodeByText(contactName)

            if (contactNode != null) {
                AccessibilityBridge.performTap(contactNode.id)
            } else {
                return false
            }

            // 6. Wait for chat screen to open
            delay(1000)
            screen = AccessibilityBridge.readCurrentScreen() ?: return false

            // 7. Find message input box and type message
            val entryNode = screen.findEditableElements().firstOrNull()
                ?: screen.findNodeByText("Message")
                ?: screen.nodes.firstOrNull { it.viewIdResourceName?.contains("entry") == true }

            if (entryNode != null) {
                AccessibilityBridge.performTypeText(message, entryNode.id)
            } else {
                AccessibilityBridge.performTypeText(message)
            }

            delay(500)

            // 8. Find and tap Send button
            clickSendButton()
        } catch (e: Exception) {
            Log.e(TAG, "Error in WhatsApp accessibility navigation", e)
            false
        }
    }

    private suspend fun clickSendButton(): Boolean {
        val screen = AccessibilityBridge.readCurrentScreen() ?: return false
        val sendNode = screen.findClickableElements().firstOrNull {
            it.viewIdResourceName?.contains("send") == true ||
                    it.contentDescription.equals("Send", ignoreCase = true) ||
                    it.contentDescription.equals("भेजें", ignoreCase = true)
        } ?: screen.findNodeByText("Send")

        return if (sendNode != null) {
            AccessibilityBridge.performTap(sendNode.id)
        } else {
            false
        }
    }

    private fun launchWhatsAppShareIntent(context: Context, message: String): Boolean {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(sendIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "WhatsApp share intent failed", e)
            false
        }
    }
}

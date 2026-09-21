package com.jarvis.app.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.jarvis.app.JarvisApplication
import com.jarvis.app.accessibility.AccessibilityBridge
import com.jarvis.app.device.DispatchResult
import kotlinx.coroutines.delay

/**
 * Autonomous Workflow Learning & Macro Replay Engine ("Teach Me Once").
 *
 * Allows the user to teach JARVIS any multi-step procedure on any app or website once.
 * JARVIS encodes the semantic steps into procedural memory, and replays them
 * autonomously upon receiving the trigger voice/text command.
 */
object WorkflowLearningEngine {

    private const val TAG = "WorkflowLearningEngine"
    private const val CATEGORY_WORKFLOW = "learned_workflow"

    data class WorkflowStep(
        val actionType: String, // OPEN_APP, OPEN_URL, CLICK, TYPE, SCROLL_DOWN, SCROLL_UP, BACK, WAIT
        val target: String,
        val param: String = ""
    )

    data class LearnedWorkflow(
        val triggerPhrase: String,
        val description: String,
        val steps: List<WorkflowStep>
    )

    /**
     * Checks if a user command is an instruction to learn a new workflow.
     */
    fun isTeachCommand(command: String): Boolean {
        val lower = command.lowercase().trim()
        return lower.startsWith("learn workflow ") ||
                lower.startsWith("learn protocol ") ||
                lower.startsWith("teach workflow ") ||
                lower.startsWith("teach protocol ") ||
                lower.startsWith("seekho ") ||
                lower.startsWith("sikho ") ||
                lower.startsWith("ye seekho ") ||
                lower.startsWith("yeh seekho ") ||
                lower.startsWith("record workflow ") ||
                (lower.contains("seekh lo ") && lower.contains("karne ka"))
    }

    /**
     * Teaches and saves a new workflow from a natural language command.
     */
    suspend fun teachWorkflow(rawCommand: String): DispatchResult {
        try {
            var body = rawCommand
                .replace("learn workflow ", "", ignoreCase = true)
                .replace("learn protocol ", "", ignoreCase = true)
                .replace("teach workflow ", "", ignoreCase = true)
                .replace("teach protocol ", "", ignoreCase = true)
                .replace("record workflow ", "", ignoreCase = true)
                .replace("ye seekho ", "", ignoreCase = true)
                .replace("yeh seekho ", "", ignoreCase = true)
                .replace("seekho ", "", ignoreCase = true)
                .replace("sikho ", "", ignoreCase = true)
                .trim()

            // Format: "{triggerPhrase}: {step1}, {step2}, {step3}" or "{triggerPhrase} -> {steps}"
            val delimiter = if (body.contains(":")) ":" else if (body.contains("->")) "->" else " then "
            val parts = body.split(delimiter, limit = 2)

            val triggerPhrase = parts[0].trim().lowercase()
            val stepsRaw = if (parts.size > 1) parts[1].trim() else ""

            if (stepsRaw.isBlank()) {
                return DispatchResult(
                    handled = true,
                    feedback = "Please provide the steps to learn, sir. Example: 'Sikho recharge: open paytm, click mobile, type 9876543210, click proceed'",
                    success = false
                )
            }

            val steps = parseSteps(stepsRaw)
            if (steps.isEmpty()) {
                return DispatchResult(
                    handled = true,
                    feedback = "Could not parse any actionable steps from your command, sir.",
                    success = false
                )
            }

            // Encode to procedural memory format: "TRIGGER::{trigger}::STEPS::{step1|target|param};;{step2|target|param}"
            val stepsEncoded = steps.joinToString(";;") { "${it.actionType}|${it.target}|${it.param}" }
            val memoryContent = "TRIGGER::$triggerPhrase::STEPS::$stepsEncoded"

            JarvisApplication.memoryRepository.remember(
                content = memoryContent,
                category = CATEGORY_WORKFLOW,
                importance = 10,
                tags = "workflow,$triggerPhrase"
            )

            return DispatchResult(
                handled = true,
                feedback = "Autonomous workflow '$triggerPhrase' learned and stored in OmX Vault with ${steps.size} steps! Simply say '$triggerPhrase' anytime to execute it, sir.",
                success = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error teaching workflow", e)
            return DispatchResult(handled = true, feedback = "Error learning workflow: ${e.message}", success = false)
        }
    }

    /**
     * Checks if the user command matches any previously learned workflow and executes it.
     */
    suspend fun tryExecuteLearnedWorkflow(rawCommand: String, context: Context): DispatchResult? {
        val lower = rawCommand.lowercase().trim()
        val memories = try {
            JarvisApplication.memoryRepository.getAllMemories()
        } catch (_: Exception) {
            return null
        }

        val workflowMemories = memories.filter { it.category == CATEGORY_WORKFLOW && it.content.startsWith("TRIGGER::") }

        for (mem in workflowMemories) {
            val parts = mem.content.split("::")
            if (parts.size >= 4 && parts[0] == "TRIGGER" && parts[2] == "STEPS") {
                val trigger = parts[1].trim().lowercase()
                val stepsEncoded = parts[3].trim()

                // Check match
                val isMatch = lower == trigger ||
                        lower == "execute $trigger" ||
                        lower == "run $trigger" ||
                        lower == "chalao $trigger" ||
                        lower == "$trigger chalao" ||
                        lower == "$trigger karo"

                if (isMatch) {
                    val steps = decodeSteps(stepsEncoded)
                    val workflow = LearnedWorkflow(trigger, "User learned autonomous protocol", steps)
                    return executeWorkflow(workflow, context)
                }
            }
        }

        return null
    }

    private fun parseSteps(stepsRaw: String): List<WorkflowStep> {
        val rawList = stepsRaw.split(Regex("[,;]|\\bthen\\b|\\bfir\\b|\\baur\\b", RegexOption.IGNORE_CASE))
        val result = mutableListOf<WorkflowStep>()

        for (item in rawList) {
            val step = item.trim()
            if (step.isBlank()) continue
            val lower = step.lowercase()

            when {
                lower.startsWith("open url ") || lower.startsWith("website ") || lower.startsWith("site ") -> {
                    val url = step.replace(Regex("^(open url|website|site)\\s+", RegexOption.IGNORE_CASE), "").trim()
                    result.add(WorkflowStep("OPEN_URL", url))
                }
                lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("kholo ") || lower.endsWith(" kholo") -> {
                    val app = step.replace(Regex("^(open|launch|kholo)\\s+", RegexOption.IGNORE_CASE), "")
                        .replace(" kholo", "", ignoreCase = true)
                        .replace(" app", "", ignoreCase = true)
                        .trim()
                    result.add(WorkflowStep("OPEN_APP", app))
                }
                lower.startsWith("click ") || lower.startsWith("tap ") || lower.contains(" pe click") || lower.contains(" par click") || lower.contains(" dabao") -> {
                    val target = step.replace(Regex("^(click on|click|tap on|tap)\\s+", RegexOption.IGNORE_CASE), "")
                        .replace(" pe click karo", "", ignoreCase = true)
                        .replace(" par click karo", "", ignoreCase = true)
                        .replace(" pe click", "", ignoreCase = true)
                        .replace(" par click", "", ignoreCase = true)
                        .replace(" dabao", "", ignoreCase = true)
                        .trim()
                    result.add(WorkflowStep("CLICK", target))
                }
                lower.startsWith("type ") || lower.startsWith("enter ") || lower.contains(" likho") || lower.contains(" daalo") -> {
                    val text = step.replace(Regex("^(type|enter)\\s+", RegexOption.IGNORE_CASE), "")
                        .replace(" likho", "", ignoreCase = true)
                        .replace(" daalo", "", ignoreCase = true)
                        .trim()
                    result.add(WorkflowStep("TYPE", text, text))
                }
                lower.contains("scroll down") || lower.contains("niche scroll") -> {
                    result.add(WorkflowStep("SCROLL_DOWN", ""))
                }
                lower.contains("scroll up") || lower.contains("upar scroll") -> {
                    result.add(WorkflowStep("SCROLL_UP", ""))
                }
                lower == "back" || lower == "go back" || lower == "piche" -> {
                    result.add(WorkflowStep("BACK", ""))
                }
                lower.startsWith("wait ") || lower.startsWith("ruko ") -> {
                    val sec = step.replace(Regex("^(wait|ruko)\\s+", RegexOption.IGNORE_CASE), "").replace("s", "").trim()
                    val ms = (sec.toLongOrNull() ?: 1L) * 1000L
                    result.add(WorkflowStep("WAIT", ms.toString()))
                }
                else -> {
                    // Default treat as click if it looks like a button/label
                    result.add(WorkflowStep("CLICK", step))
                }
            }
        }

        return result
    }

    private fun decodeSteps(encoded: String): List<WorkflowStep> {
        val result = mutableListOf<WorkflowStep>()
        val stepItems = encoded.split(";;")
        for (item in stepItems) {
            val tokens = item.split("|")
            if (tokens.size >= 2) {
                val action = tokens[0]
                val target = tokens[1]
                val param = if (tokens.size >= 3) tokens[2] else ""
                result.add(WorkflowStep(action, target, param))
            }
        }
        return result
    }

    /**
     * Executes the sequence of steps on the device autonomously.
     */
    suspend fun executeWorkflow(workflow: LearnedWorkflow, context: Context): DispatchResult {
        Log.i(TAG, "Executing learned workflow: ${workflow.triggerPhrase} with ${workflow.steps.size} steps")

        for ((index, step) in workflow.steps.withIndex()) {
            when (step.actionType.uppercase()) {
                "OPEN_APP" -> {
                    val appTarget = step.target.lowercase()
                    val pm = context.packageManager
                    val targetPkg = when (appTarget) {
                        "youtube", "yt" -> "com.google.android.youtube"
                        "whatsapp", "wa" -> "com.whatsapp"
                        "chrome", "browser" -> "com.android.chrome"
                        "paytm" -> "net.one97.paytm"
                        "zomato" -> "com.application.zomato"
                        "swiggy" -> "in.swiggy.android"
                        "instagram" -> "com.instagram.android"
                        "settings" -> "com.android.settings"
                        else -> null
                    }

                    val launchIntent = if (targetPkg != null) {
                        pm.getLaunchIntentForPackage(targetPkg)
                    } else {
                        val installed = pm.getInstalledApplications(0)
                        val found = installed.firstOrNull { pm.getApplicationLabel(it).toString().contains(appTarget, ignoreCase = true) }
                        found?.let { pm.getLaunchIntentForPackage(it.packageName) }
                    }

                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        delay(1500)
                    }
                }
                "OPEN_URL" -> {
                    val url = if (step.target.startsWith("http")) step.target else "https://${step.target}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    delay(1500)
                }
                "CLICK", "TAP" -> {
                    delay(800)
                    val screen = AccessibilityBridge.readCurrentScreen()
                    if (screen != null) {
                        val node = screen.findNodeByText(step.target)
                            ?: screen.findClickableElements().firstOrNull {
                                it.text?.contains(step.target, ignoreCase = true) == true ||
                                        it.contentDescription?.contains(step.target, ignoreCase = true) == true
                            }
                        if (node != null) {
                            AccessibilityBridge.performTap(node.id)
                        }
                    }
                    delay(600)
                }
                "TYPE" -> {
                    delay(600)
                    val textToType = step.param.ifBlank { step.target }
                    val screen = AccessibilityBridge.readCurrentScreen()
                    val targetInput = screen?.findEditableElements()?.firstOrNull()
                    if (targetInput != null) {
                        AccessibilityBridge.performTypeText(textToType, targetInput.id)
                    } else {
                        AccessibilityBridge.performTypeText(textToType)
                    }
                    delay(500)
                }
                "SCROLL_DOWN" -> {
                    AccessibilityBridge.performScroll("DOWN")
                    delay(600)
                }
                "SCROLL_UP" -> {
                    AccessibilityBridge.performScroll("UP")
                    delay(600)
                }
                "BACK" -> {
                    AccessibilityBridge.performBack()
                    delay(600)
                }
                "WAIT" -> {
                    val ms = step.target.toLongOrNull() ?: 1000L
                    delay(ms)
                }
            }
        }

        return DispatchResult(
            handled = true,
            feedback = "Autonomous learned protocol '${workflow.triggerPhrase}' executed successfully with ${workflow.steps.size} steps, sir.",
            success = true
        )
    }
}

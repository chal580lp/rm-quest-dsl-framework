package com.runemate.rebirther.quest

import com.runemate.corev2.utility.InterfaceUtil.exitQuestCompletionInterface
import com.runemate.game.api.hybrid.local.Varbits
import com.runemate.game.api.hybrid.local.Varps
import com.runemate.rebirther.Bot
import com.runemate.rebirther.baseclasses.RequiredItem
import com.runemate.rebirther.handler.ItemManager
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.quest.builder.QuestProperties
import com.runemate.rebirther.quest.builder.QuestValueType
import com.runemate.rebirther.quest.framework.QuestStep
import com.runemate.rebirther.quest.framework.QuestStepState.*
import org.apache.logging.log4j.Logger
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

abstract class QuestDetail(
	protected val bot: Bot,
	val PROPERTIES: QuestProperties,
) {
	protected abstract val log: Logger
	protected abstract val steps: List<QuestStep>
	private var currentStepState = VERIFY_ITEMS

	private val itemManager = ItemManager(bot)
	private val restockItems: List<RequiredItem> by lazy {
		(PROPERTIES.requiredItems + PROPERTIES.teleportItems).filter { it.alwaysRestock }
	}
	open val addListener = false

	open fun isEligible(): Boolean = PROPERTIES.requirements.all { it.isMet() }
	open fun isComplete(): Boolean = getProgressValue() == PROPERTIES.completionValue && exitQuestCompletionInterface()

	fun getProgressValue(): Int {
		return when (PROPERTIES.valueType) {
            QuestValueType.VARP -> Varps.getAt(PROPERTIES.questValue)
            QuestValueType.VARBIT -> Varbits.load(PROPERTIES.questValue)
                ?
                ?: -1
		}
	}

	open fun onStart() {
		log.info("onStart ${PROPERTIES.name}")
	}

	open fun onFinish() {
		log.info("onFinish ${PROPERTIES.name}")
	}

	open fun execute(): Boolean {
		val currentVarpValue = getProgressValue()
        val currentStep = steps.find { it == currentVarpValue }
		if (currentStep == null) {
			log.warn("No step found for varp value: $currentVarpValue")
			return true
		}
		return executeStep(currentStep)
	}


	private fun executeStep(step: QuestStep): Boolean {
        bot.ui.updateStatus("${PROPERTIES.name} step: ${step}")
        //log.debug("Executing step: ${step}")

		// VERIFY_ITEMS
		if (currentStepState == VERIFY_ITEMS) {
            log.debug("VERIFY_ITEMS step: ${step}")
            if (step.items.isNotEmpty() && !verifyItems("Step :${step}", step.items)) {
				return false
			}
			currentStepState = CHECK_CONDITION
		}

		// CHECK_CONDITION
		if (currentStepState == CHECK_CONDITION) {
            log.debug("CHECK_CONDITION step: ${step}")
			if (!step.entry()) {
                log.debug("Entry condition not met for step: ${step}")
				if (step.fallback == null) {
                    log.warn("No fallback action for step: ${step}")
					return false
				}
                log.info("Executing fallback action for step: ${step}")
				step.fallback.invoke()
				currentStepState = VERIFY_ITEMS
				return false
			}
			currentStepState = EXECUTE_ACTION
		}

		// EXECUTE_ACTION
		if (currentStepState == EXECUTE_ACTION) {
			val operations = step.operation?.let { listOf(it) } ?: step.operations ?: emptyList()
            log.debug("EXECUTE_ACTION step: ${step} with ${operations.size} operations")

			for (operation in operations) {
				if (!operation.entry()) {
					log.debug("Fallback is null: ${operation.fallback == null}")
					operation.fallback?.let { fallback ->
						log.debug("FALLBACK for operation: ${operation.name}")
						fallback()
						return false
					}
					continue
				}

				if (operation.items.isNotEmpty() && !verifyItems("Operation: ${operation.name}", operation.items)) {
					log.info("Fetching items for operation: ${operation.name}")
					return false
				}

				log.info("EXECUTE_OPERATION: ${operation.name}")
				operation.action()

				operation.exit?.let { it() }

				return false
			}

            val warning = if (operations.isEmpty()) "No operations found for step: ${step}"
            else "No operations executed for step: ${step}"
			log.warn(warning)
		}

		// COMPLETE
		if (currentStepState == COMPLETE) {
            log.info("COMPLETE: ${step}")
			currentStepState = VERIFY_ITEMS
			return isComplete()
		}

		return false
	}


	private fun verifyItems(name: String?, items: List<RequiredItem>): Boolean {
		if (items.isEmpty()) return true
		if (itemManager.verifyAndFetchItems(items, restockItems)) {
			log.debug("Items verified for $name")
			return true
		} else {
			log.debug("Fetching items for {}", name)
			for (requiredItem in items) {
				log.info("Fetching item: ${requiredItem.itemDetail.name} x${requiredItem.quantity}")
			}
			return false
		}
	}

	open fun navigateToStartLocation() {
		Navigation.viaEnhToSceneToWeb(PROPERTIES.startLocation)
	}

	abstract fun atStartLocation(): Boolean

	protected fun repeatStep(stepValue: Int) {
		log.debug("Repeating step: $stepValue")
        val step = steps.find { it == stepValue }
		step?.let {
			executeStep(it)
		} ?: log.error("Could not find step to repeat: $stepValue")
	}

	fun executeDebugFunction(functionName: String) {
		try {
			val method = this::class.memberFunctions.find { it.name == functionName }
			if (method != null) {
				method.isAccessible = true
				log.info("Attempting to execute debug function: $functionName")
				method.call(this)
				log.info("Successfully executed debug function: $functionName")
			} else {
				log.warn("Debug function not found: $functionName")
			}
		} catch (e: Exception) {
			log.error("Error executing debug function $functionName: ${e.message}")
			log.error("Stack trace:", e)
			e.printStackTrace() // This will print the full stack trace to standard error
		}
	}
}





package com.runemate.rebirther.quest

import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.local.AccountInfo
import com.runemate.game.api.hybrid.local.hud.interfaces.Bank
import com.runemate.game.api.script.framework.AbstractBot
import com.runemate.game.api.script.framework.listeners.VarpListener
import com.runemate.game.api.script.framework.listeners.events.VarpEvent
import com.runemate.rebirther.handler.ItemManager

class QuestHandler(private val bot: AbstractBot, private val quest: QuestDetail) : VarpListener {
	private val log = getLogger("QuestHandler")
	private var state: QuestHandlerState = QuestHandlerState.CHECK_STARTING_PROGRESSION
	private val itemManager = ItemManager(bot)
	private var hasStarted = false
	private var hasFinished = false

	enum class QuestHandlerState {
		CHECK_STARTING_PROGRESSION,
		CHECK_AND_FETCH_REQUIRED_ITEMS,
		NAVIGATE_TO_START_LOCATION,
		EXECUTE_QUEST_STEPS,
		COMPLETED,
		FAILED,
	}

	fun execute() {
		when (state) {
			QuestHandlerState.CHECK_STARTING_PROGRESSION -> checkStartingProgression()
			QuestHandlerState.CHECK_AND_FETCH_REQUIRED_ITEMS -> checkAndFetchRequiredItems()
			QuestHandlerState.NAVIGATE_TO_START_LOCATION -> navigateToStartLocation()
			QuestHandlerState.EXECUTE_QUEST_STEPS -> executeSteps()
			QuestHandlerState.COMPLETED -> handleCompletion()
			QuestHandlerState.FAILED -> handleFailure()
		}
	}

	private fun executeSteps() {
		if (!hasStarted) {
			quest.onStart()
			hasStarted = true
		}

		quest.execute()

		if (quest.isComplete()) {
			log.info("Quest execution completed")
			state = QuestHandlerState.COMPLETED
		}
	}

	private fun handleCompletion() {
		if (!hasFinished) {
			quest.onFinish()
			hasFinished = true
			log.info("Quest ${quest.PROPERTIES.name} completed")
		}
	}

	private fun handleFailure() {
		log.error("Quest ${quest.PROPERTIES.name} failed")
	}

	fun isCompleted(): Boolean = state == QuestHandlerState.COMPLETED

	fun checkRequirements(): Boolean {
		log.debug("Checking requirements for quest: ${quest.PROPERTIES.name}")
		if (quest.isComplete()) {
			log.info("Quest: ${quest.PROPERTIES.name} has already been completed")
			return false
		}
		if (!quest.isEligible()) {
			log.info("Requirements not met for quest: ${quest.PROPERTIES.name}")
			quest.PROPERTIES.requirements.forEach {
				log.info("Requirement: $it")
			}
			return false
		}
		return true
	}

	fun onStart() {
		bot.eventDispatcher.addListener(this)
	}

	fun onFinish() {
		bot.eventDispatcher.removeListener(this)
	}

	private fun checkStartingProgression() {
		if (quest.getProgressValue() != 0 || quest.PROPERTIES.skipCheck) {
			log.info("Skipping to execution: ${if (quest.PROPERTIES.skipCheck) "skipCheck is enabled" else "quest has already been started"}")
			//log.info("Quest has already been started, skipping to execution")
			state = QuestHandlerState.EXECUTE_QUEST_STEPS
		} else {
			state = QuestHandlerState.CHECK_AND_FETCH_REQUIRED_ITEMS
		}
	}

	private fun checkAndFetchRequiredItems() {
		log.debug("Checking and fetching required items for quest: ${quest.PROPERTIES.name}")
		val items = buildList {
			addAll(quest.PROPERTIES.requiredItems)
			if (AccountInfo.isMember()) {
				addAll(quest.PROPERTIES.teleportItems)
			}
		}

		if (itemManager.verifyAndFetchItems(items)) {
			log.info("All required items are available")
			state = QuestHandlerState.NAVIGATE_TO_START_LOCATION
		} else {
			log.info("Still missing some items, will try again next iteration")
			// The state remains as CHECK_REQUIRED_ITEMS, so it will try again in the next iteration
		}
	}

	private fun navigateToStartLocation() {
		if (Bank.isOpen()) Bank.close()
		quest.navigateToStartLocation()
		if (quest.atStartLocation()) {
			log.info("Arrived at quest start location, starting quest: ${quest.PROPERTIES.name}")
			state = QuestHandlerState.EXECUTE_QUEST_STEPS
		}
	}

	override fun onValueChanged(event: VarpEvent?) {
		if (event?.varp?.index == quest.PROPERTIES.questValue) {
			log.info("Quest progression updated from ${event.oldValue} to: ${event.newValue}")
			if (event.newValue == quest.PROPERTIES.completionValue) {
				log.info("Quest: ${quest.PROPERTIES.name} has been completed")
				state = QuestHandlerState.COMPLETED
			}
			if (event.newValue < event.oldValue) {
				log.warn("Quest progression has gone backwards from ${event.oldValue} to: ${event.newValue}")
				state = QuestHandlerState.CHECK_AND_FETCH_REQUIRED_ITEMS
			}
			bot.pause()
		}
	}
}
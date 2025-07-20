package com.runemate.rebirther.quest.free

import com.runemate.corev2.extensions.climbUp
import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.entities.Npc
import com.runemate.game.api.hybrid.local.Camera
import com.runemate.game.api.hybrid.local.Skill
import com.runemate.game.api.hybrid.local.VarpID
import com.runemate.game.api.hybrid.region.GameObjects
import com.runemate.game.api.hybrid.region.Players
import com.runemate.rebirther.Bot
import com.runemate.rebirther.details.Def.Items
import com.runemate.rebirther.details.npc.type.QuestNPC
import com.runemate.rebirther.handler.NpcInteractionHandler.Companion.talkToNPC
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.navigate.Navigation.toWizardTowerFloor
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.*
import com.runemate.rebirther.quest.framework.Operation
import com.runemate.rebirther.quest.framework.QuestStep
import com.runemate.rebirther.util.QuestUtil.requiredItem
import com.runemate.rebirther.util.QuestUtil.requiredItems


private object ImpCatcherProperties {
	val properties = quest {
		info(
			name = "Imp Catcher",
			description = "",
			startRegion = GameRegion.WIZARDS_TOWER,
			startLocation = QuestNPC.WIZARD_MIZGOG.location,
			questValue = VarpID.QUEST_IMP_CATCHER.id,
			completionValue = 2
		)
		reward(
			questPoints = 1,
			experience = mapOf(Skill.MAGIC to 875)
		)
		npcs {
			friendly(QuestNPC.WIZARD_MIZGOG)
		}

		items {
			inventory(Items.QUEST.RED_BEAD)
			inventory(Items.QUEST.YELLOW_BEAD)
			inventory(Items.QUEST.BLACK_BEAD)
			inventory(Items.QUEST.WHITE_BEAD)
			teleport(Items.TELEPORT.NECKLACE_OF_PASSAGE)
			teleport(Items.TELEPORT.RING_OF_WEALTH)
		}
	}
}

class ImpCatcher(bot: Bot) : QuestDetail(bot, ImpCatcherProperties.properties) {
	override val log = getLogger("ImpCatcher")

	private val wizardMizgog: Npc?
		get() = QuestNPC.WIZARD_MIZGOG.get()


	override val steps = listOf(
		QuestStep(
			name = "Start the quest",
			value = 0,
			entry = { wizardMizgog != null },
			operation = Operation(
				name = "Talk to Wizard Mizgog",
				action = { startQuest() }
			)
		),
		QuestStep(
			name = "Hand in beads",
			value = 1,
			items = requiredItems(
				requiredItem(Items.QUEST.RED_BEAD, 1),
				requiredItem(Items.QUEST.YELLOW_BEAD, 1),
				requiredItem(Items.QUEST.BLACK_BEAD, 1),
				requiredItem(Items.QUEST.WHITE_BEAD, 1)
			),
			entry = { wizardMizgog != null },
			operation = Operation(
				name = "Hand in beads",
				action = { handInBeads() }
			)
		)
	)

	private fun startQuest() {
		log.info("Starting Imp Catcher")
		if (wizardMizgog?.isVisible == false) Camera.turnTo(wizardMizgog)
		talkToNPC(wizardMizgog, listOf("Give me a quest please.", "Yes."))
	}

	private fun handInBeads() {
		log.info("Handing in beads")
		talkToNPC(wizardMizgog)
	}

	override fun navigateToStartLocation() {
		traverseToWizard()
	}

	override fun atStartLocation(): Boolean {
		return wizardMizgog != null
	}

	private fun traverseToWizard() {
		log.info("Taversing to Wizard Mizgog")
		if (!PROPERTIES.startRegion.isPlayerInRegion()) {
			Navigation.toRegion(PROPERTIES.startRegion)
		} else {
			when (Players.getLocal()?.position?.plane) {
				0 -> {
					if (GameRegion.WIZARDS_TOWER.isPlayerInRegion()) {
						toWizardTowerFloor(2)
					}
				}

				1 -> {
					GameObjects.newQuery().names("Staircase").actions("Climb-up").results().nearest()?.climbUp()
					Delay.until({ Players.getLocal()?.position?.plane == 2 }, 1200)
				}
			}
		}
	}
}

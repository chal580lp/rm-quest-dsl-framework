package com.runemate.rebirther.quest.free

import com.runemate.corev2.extensions.open
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.entities.Npc
import com.runemate.game.api.hybrid.local.Skill
import com.runemate.game.api.hybrid.local.VarpID
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.hybrid.region.GameObjects
import com.runemate.rebirther.Bot
import com.runemate.rebirther.details.Def
import com.runemate.rebirther.details.npc.type.QuestNPC
import com.runemate.rebirther.handler.NpcInteractionHandler.Companion.talkToNPC
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.*
import com.runemate.rebirther.quest.framework.Operation
import com.runemate.rebirther.quest.framework.QuestStep
import com.runemate.rebirther.util.QuestUtil.requiredItem
import com.runemate.rebirther.util.QuestUtil.requiredItems


private object SheepShearerProperties {
	val properties = quest {
		info(
			name = "Sheep Shearer",
			description = "",
			startRegion = GameRegion.LUMBRIDGE,
			startLocation = QuestNPC.FRED_THE_FARMER.location,
			questValue = VarpID.QUEST_SHEEP_SHEARER.id,
			completionValue = 21 // 20 wool + 1 quest completion
		)
		reward(
			questPoints = 1,
			experience = mapOf(Skill.CRAFTING to 150)
		)
		npcs {
			friendly(QuestNPC.FRED_THE_FARMER)
		}

		items {
			inventory(Def.Items.QUEST.BALL_OF_WOOL, 20)
			teleport(Def.Items.TELEPORT.LUMBRIDGE_TELEPORT_TAB, withdraw = 5)
			teleport(Def.Items.TELEPORT.RING_OF_WEALTH)
		}
	}
	val FRED_THE_FARMER_ENTRANCE = Coordinate(3188, 3280, 0)
}

class SheepShearer(bot: Bot) : QuestDetail(bot, SheepShearerProperties.properties) {
	override val log = getLogger("SheepShearer")

	private val fredTheFarmer: Npc?
		get() = QuestNPC.FRED_THE_FARMER.get()

	override val steps = listOf(
		QuestStep(
			name = "Start the quest",
			value = 0,
			entry = { QuestNPC.FRED_THE_FARMER.isVisibleReachable() },
			fallback = { navigateToStartLocation() },
			operation = Operation(
				name = "Talk to Fred the Farmer",
				action = { startQuest() },
			)
		),
		QuestStep(
			name = "Deliver items to Fred",
			value = 1,
			items = requiredItems(
				requiredItem(Def.Items.QUEST.BALL_OF_WOOL, 20)
			),
			entry = { QuestNPC.FRED_THE_FARMER.get()?.isVisible == true },
			fallback = { navigateToStartLocation() },
			operation = Operation(
				name = "Deliver items",
				action = { deliverItems() }
			)
		)
	)

	private fun startQuest() {
		log.info("Starting quest: ${PROPERTIES.name} talk to $fredTheFarmer")
		talkToNPC(fredTheFarmer, listOf("I'm looking for a quest.", "Yes."))
	}

	private fun deliverItems() {
		log.info("Delivering items for quest: ${PROPERTIES.name}")
		talkToNPC(fredTheFarmer, listOf("I have some wool for you."))
	}

	override fun navigateToStartLocation() {
		log.debug("Traversing to the Fred the Farmer")
		if (!QuestNPC.FRED_THE_FARMER.gameRegion.isPlayerInRegion()) {
			Navigation.toRegion(QuestNPC.FRED_THE_FARMER.gameRegion)
		} else {
			val gate =
				GameObjects.newQuery().names("Gate").on(Coordinate(3188, 3279, 0)).actions("Open").results().first()
			val door =
				GameObjects.newQuery().names("Door").on(Coordinate(3189, 3275, 0)).actions("Open").results().first()
			if (gate != null) {
				gate.open()
			} else if (door != null) {
				door.open()
			} else {
				Navigation.toCoord(SheepShearerProperties.FRED_THE_FARMER_ENTRANCE)
			}
		}
	}

	override fun atStartLocation(): Boolean {
		return QuestNPC.FRED_THE_FARMER.isVisibleReachable()
	}

}
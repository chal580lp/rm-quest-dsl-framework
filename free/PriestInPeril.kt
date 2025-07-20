package com.runemate.rebirther.quest.free

import com.runemate.corev2.item.BaseItem
import com.runemate.corev2.item.Food
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.local.Quest
import com.runemate.game.api.hybrid.local.Skill
import com.runemate.game.api.hybrid.local.VarpID
import com.runemate.game.api.hybrid.location.Area
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.rebirther.Bot
import com.runemate.rebirther.baseclasses.RequiredItem.StorageType
import com.runemate.rebirther.details.Def
import com.runemate.rebirther.details.npc.NPCDatabase
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.*
import com.runemate.rebirther.quest.framework.Operation
import com.runemate.rebirther.quest.framework.QuestStep
import com.runemate.rebirther.quest.free.PriestInPerilProperties.KING_ROALD
import com.runemate.rebirther.quest.free.PriestInPerilProperties.PATERDOMUS_TEMPLE_ENTRANCE

private object PriestInPerilProperties {

	val KING_ROALD: NPCDatabase.QuestNPCDetail = NPCDatabase.QuestNPCDetail(
		name = "King Roald",
		ids = setOf(648),
		gameRegion = GameRegion.VARROCK,
		location = Area.Rectangular(Coordinate(3224, 3474, 0), Coordinate(3219, 3470, 0)),
		quests = setOf(Quest.OSRS.PRIEST_IN_PERIL),
	)

	val properties = quest {
		info(
			name = "Priest in Peril",
			description = "",
			startRegion = GameRegion.VARROCK,
			startLocation = KING_ROALD.location,
			questValue = VarpID.QUEST_PRIEST_IN_PERIL.id,
			completionValue = 5,
		)
		reward(
			questPoints = 1,
			experience = mapOf(Skill.PRAYER to 1406),
			items = setOf(BaseItem("Wolfbane", -1)),
			other = setOf("Access to Morytania")
		)
		npcs {
			friendly(KING_ROALD)
		}
		items {
			bank(Def.Items.SKILL.PURE_ESSENCE, 50, storage = StorageType.BANK)
			bank(Def.Items.GENERAL.BUCKET)
			teleport(Def.Items.TELEPORT.VARROCK_TELEPORT_TAB, withdraw = 5)
			teleport(Def.Items.TELEPORT.RING_OF_DUELING)
		}
		combat(
			food = Food.Lobster,
		)
	}

	val PATERDOMUS_TEMPLE_ENTRANCE = Area.Rectangular(Coordinate(3407, 3490, 0), Coordinate(3405, 3487, 0))
}

class PriestInPeril(bot: Bot) : QuestDetail(bot, PriestInPerilProperties.properties) {
	override val log = getLogger("PriestInPeril*")

	override fun navigateToStartLocation() {
		Navigation.toLocationInRegion(PROPERTIES.startLocation, PROPERTIES.startRegion)
	}

	override fun atStartLocation(): Boolean {
		return KING_ROALD.isVisibleReachable()
	}

	override val steps: List<QuestStep> = listOf(
		QuestStep(
			name = "Speak to King Roald in Varrock Palace.",
			value = 0,
			entry = { true },
			operation = Operation(
				action = { KING_ROALD.talkTo() }
			)
		),
		QuestStep(
			name = "Speak to Drezel then kill the Temple Guardian.",
			value = 1,
			entry = { true },
			food = 20,
			operation = Operation(
				entry = { true },
				action = { enterTemple() }
			)
		),
	)

	private fun enterTemple() {
		// if temple door is not null open

		Navigation.viaEnhanced(PATERDOMUS_TEMPLE_ENTRANCE)
	}
}
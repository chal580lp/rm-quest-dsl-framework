package com.runemate.rebirther.quest.free

import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.local.Skill
import com.runemate.game.api.hybrid.local.VarpID
import com.runemate.game.api.hybrid.location.Area
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.rebirther.Bot
import com.runemate.rebirther.details.Def.Items
import com.runemate.rebirther.details.npc.type.QuestNPC
import com.runemate.rebirther.handler.NpcInteractionHandler.Companion.talkToNPC
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.*
import com.runemate.rebirther.quest.framework.Operation
import com.runemate.rebirther.quest.framework.QuestStep

private object DoricsQuestProperties {
	val properties = quest {
		info(
			name = "Doric's Quest",
			description = "",
			startRegion = GameRegion.FALADOR.ABOVE_TO_WILDERNESS,
			startLocation = Area.Rectangular(Coordinate(2953, 3449, 0), Coordinate(2950, 3454, 0)),
			questValue = VarpID.QUEST_DORICS_QUEST.id,
			completionValue = 100
		)
		reward(
			questPoints = 1,
			experience = mapOf(Skill.MINING to 1300),
		)
		npcs {
			friendly(QuestNPC.DORIC)
		}
		items {
			inventory(Items.SKILL.IRON_ORE, 2)
			inventory(Items.SKILL.COPPER_ORE, 4)
			inventory(Items.SKILL.CLAY, 6)
			teleport(Items.TELEPORT.FALADOR_TELEPORT_TAB, quantity = 1, withdraw = 5)
			teleport(Items.TELEPORT.RING_OF_WEALTH)
		}
	}
}

class DoricsQuest(bot: Bot) : QuestDetail(bot, DoricsQuestProperties.properties) {
	override val log = getLogger("DoricsQuest")
	private val outsideDoricsShed = Coordinate(2948, 3450, 0)
	override val steps: List<QuestStep> = listOf(
		QuestStep(
			name = "Talk to Doric to start the quest.",
			value = 0,
			entry = { QuestNPC.DORIC.get()?.isVisible == true },
			operation = Operation(
				action = { talkToDoric() },
				fallback = { navigateToStartLocation() }
			)
		),
		QuestStep(
			name = "Bring Doric the ores he requested.",
			value = 10,
			entry = { QuestNPC.DORIC.get()?.isVisible == true },
			operation = Operation(
				action = { talkToDoric() },
				fallback = { navigateToStartLocation() }
			)
		)
	)

	private fun talkToDoric() {
		talkToNPC(
			QuestNPC.DORIC.get(),
			listOf("I wanted to use your anvils.", "Yes.", "Certainly, I'll be right back!")
		)
	}

	override fun navigateToStartLocation() {
		if (!GameRegion.FALADOR.isPlayerInRegion() && !GameRegion.FALADOR.ABOVE_TO_WILDERNESS.isPlayerInRegion()) {
			log.debug("Walking to Falador")
			Navigation.toRegion(PROPERTIES.startRegion)
		} else {
			log.debug("Walking to Dorics shed")
			Navigation.toArea(PROPERTIES.startLocation)
		}
	}

	override fun atStartLocation(): Boolean {
		QuestNPC.DORIC.get()?.let {
			return it.isVisible && it.area?.isReachable == true
		}
		return false
	}
}
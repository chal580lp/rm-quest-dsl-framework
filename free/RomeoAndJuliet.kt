package com.runemate.rebirther.quest.free

import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.RuneScape
import com.runemate.game.api.hybrid.local.Quest
import com.runemate.game.api.hybrid.local.VarpID
import com.runemate.game.api.hybrid.local.hud.interfaces.ChatDialog
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory
import com.runemate.game.api.hybrid.location.Area
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.rebirther.Bot
import com.runemate.rebirther.details.Def.Items
import com.runemate.rebirther.details.npc.NPCDatabase
import com.runemate.rebirther.handler.NpcInteractionHandler.Companion.talkToNPC
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.info
import com.runemate.rebirther.quest.builder.items
import com.runemate.rebirther.quest.builder.quest
import com.runemate.rebirther.quest.builder.reward
import com.runemate.rebirther.quest.framework.Operation
import com.runemate.rebirther.quest.framework.Operation.Companion.continueDialog
import com.runemate.rebirther.quest.framework.QuestStep
import com.runemate.rebirther.quest.free.RomeoAndJulietProperties.APOTHECARY
import com.runemate.rebirther.quest.free.RomeoAndJulietProperties.FATHER_LAWRENCE
import com.runemate.rebirther.quest.free.RomeoAndJulietProperties.JULIET
import com.runemate.rebirther.quest.free.RomeoAndJulietProperties.JULIET_DOOR_INNER
import com.runemate.rebirther.quest.free.RomeoAndJulietProperties.JULIET_DOOR_OUTER
import com.runemate.rebirther.quest.free.RomeoAndJulietProperties.ROMEO
import com.runemate.rebirther.util.ObjectInteraction
import com.runemate.rebirther.util.ObjectUtil

private object RomeoAndJulietProperties {

	val ROMEO = NPCDatabase.QuestNPCDetail(
		name = "Romeo",
		ids = setOf(5037),
		gameRegion = GameRegion.VARROCK,
		location = Coordinate(3213, 3421, 0).area,
		quests = setOf(Quest.OSRS.ROMEO_AND_JULIET),
	)

	val JULIET = NPCDatabase.QuestNPCDetail(
		name = "Juliet",
		ids = setOf(6268),
		gameRegion = GameRegion.VARROCK,
		location = Coordinate(3158, 3425, 1).area,
		quests = setOf(Quest.OSRS.ROMEO_AND_JULIET),
	)

	val FATHER_LAWRENCE = NPCDatabase.QuestNPCDetail(
		name = "Father Lawrence",
		ids = setOf(5038),
		gameRegion = GameRegion.VARROCK,
		location = Coordinate(3257, 3484, 0).area,
		quests = setOf(Quest.OSRS.ROMEO_AND_JULIET),
	)

	val APOTHECARY = NPCDatabase.QuestNPCDetail(
		name = "Apothecary",
		ids = setOf(5036),
		gameRegion = GameRegion.VARROCK,
		location = Coordinate(3196, 3404, 0).area,
		quests = setOf(Quest.OSRS.ROMEO_AND_JULIET),
	)

	val properties = quest {
		info(
			name = "Romeo and Juliet",
			description = "",
			startRegion = GameRegion.VARROCK,
			startLocation = ROMEO.location,
			questValue = VarpID.QUEST_ROMEO_AND_JULIET.id,
			completionValue = 100
		)
		reward(
			questPoints = 5,
		)
		items {
			teleport(Items.TELEPORT.VARROCK_TELEPORT_TAB, quantity = 1, withdraw = 5)
			teleport(Items.TELEPORT.RING_OF_WEALTH)
		}
	}

	val JULIET_DOOR_OUTER = ObjectInteraction(
		name = "Door",
		action = "Open",
		coordinate = Coordinate(3157, 3430, 1)
	)

	val JULIET_DOOR_INNER = ObjectInteraction(
		name = "Door",
		action = "Open",
		coordinate = Coordinate(3158, 3426, 1)
	)
}

class RomeoAndJuliet(bot: Bot) : QuestDetail(bot, RomeoAndJulietProperties.properties) {
	override val log = getLogger("RomeoAndJuliet")

	override fun navigateToStartLocation() {
		Navigation.toLocationInRegion(PROPERTIES.startLocation, PROPERTIES.startRegion)
	}

	override fun atStartLocation(): Boolean {
		return ROMEO.get() != null
	}

	override val steps: List<QuestStep> = listOf(
		QuestStep(
			name = "Talk to Romeo to start the quest.",
			value = 0,
			entry = { ROMEO.get() != null },
			operation = Operation(
				action = {
					talkToNPC(
						ROMEO.get(), listOf(
							"Perhaps I could help to find her for you?",
							"Yes, ok, I'll let her know.",
							"Yes."
						)
					)
				},
				fallback = { navigateToStartLocation() }
			)
		),
		QuestStep(
			name = "Talk to Juliet.",
			value = 10,
			entry = { JULIET.get() != null },
			fallback = { Navigation.toArea(JULIET.location) },
			operation = Operation(
				entry = { JULIET.isReachable() },
				action = {
					talkToNPC(JULIET.get())
				},
				fallback = {
					ObjectUtil.interactOrWalkToObjects(
						JULIET_DOOR_OUTER,
						JULIET_DOOR_INNER,
						teleport = false
					)
				}
			)
		),
		QuestStep(
			name = "Deliver the message to Romeo.",
			value = 20,
			entry = { ROMEO.isReachable() },
			fallback = { navigateToStartLocation() },
			operation = Operation(
				action = {
					talkToNPC(
						ROMEO.get(), listOf(
							"I have a message from Juliet.",
							"Yes, I'll tell her."
						)
					)
				},
			)
		),
		QuestStep(
			name = "Talk to Father Lawrence.",
			value = 30,
			entry = { FATHER_LAWRENCE.isReachable() },
			fallback = { Navigation.toArea(FATHER_LAWRENCE.location) },
			operation = Operation(
				name = "Talking to Father Lawrence",
				action = {
					log.debug("Talking to Father Lawrence")
					talkToNPC(
						FATHER_LAWRENCE.get(), listOf(
							"I'm looking for a potion.",
							"Ok, thanks."
						)
					)
				},
			)
		),
		QuestStep(
			name = "Talk to Apothecary with Cadava berries.",
			value = 40,
			entry = { Inventory.contains("Cadava berries") },
			fallback = {
				ObjectUtil.interactElseTraverse(
					"Cadava bush",
					"Pick-from",
					area = Area.Rectangular(Coordinate(3273, 3372, 0), Coordinate(3264, 3363, 0)),
					distance = 10,
					teleport = false
				)
			},
			operations = listOf(
				Operation(
					name = "Talking to Apothecary",
					entry = { APOTHECARY.isVisibleReachable() },
					action = {
						talkToNPC(
							APOTHECARY.get(),
							listOf(
								"Talk about something else.",
								"Talk about Romeo & Juliet.",
								"I need a Cadava potion.",
								"Ok, thanks."
							)
						)
					},
					fallback = { APOTHECARY.location.area?.grow(2, 2)?.let { Navigation.toArea(it) } }
				),
			)
		),
		QuestStep(
			name = "Finish making potion.",
			value = 50,
			entry = { Inventory.contains("Cadava berries") || Inventory.contains("Cadava potion") || RuneScape.isCutscenePlaying() },
			fallback = { repeatStep(40) },
			operations = listOf(
				Operation.continueCutscene,
				Operation(
					name = "Delivering potion to Juliet",
					entry = { JULIET.isVisibleReachable() && Inventory.contains("Cadava potion") },
					action = {
						talkToNPC(
							JULIET.get(),
							listOf(
								"I have a potion for you.",
								"Ok, thanks."
							)
						)
					},
				),
				Operation(
					entry = { !JULIET.isVisibleReachable() && Inventory.contains("Cadava potion") },
					action = { repeatStep(10) }
				),
				Operation(
					entry = { APOTHECARY.isVisibleReachable() },
					action = {
						talkToNPC(
							APOTHECARY.get()
						)
					},
					fallback = { repeatStep(40) }
				),
			)
		),
		QuestStep(
			name = "Tell Romeo to meet Juliet in the crypt.",
			value = 60,
			entry = { ROMEO.isVisibleReachable() },
			fallback = { navigateToStartLocation() },
			operations = listOf(
				continueDialog,
				Operation(
					action = {
						if (!ChatDialog.isOpen() && ROMEO.distanceToPlayer() < 3) {
							if (!Delay.until(Delay.CUTSCENE_IS_PLAYING, 2400)) {
								ROMEO.talkTo()
							}
						} else {
							ROMEO.talkTo()
						}
					},
				)
			)
		)
	)

}

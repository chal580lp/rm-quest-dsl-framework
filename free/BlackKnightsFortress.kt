package com.runemate.rebirther.quest.free

import com.runemate.corev2.extensions.distanceToPlayer
import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.RuneScape
import com.runemate.game.api.hybrid.local.VarpID
import com.runemate.game.api.hybrid.local.hud.interfaces.ChatDialog
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.hybrid.region.Players
import com.runemate.game.api.script.Execution
import com.runemate.rebirther.Bot
import com.runemate.rebirther.baseclasses.RequiredItem
import com.runemate.rebirther.details.Def
import com.runemate.rebirther.details.npc.type.QuestNPC.SIR_AMIK_VARZE
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.*
import com.runemate.rebirther.quest.free.BlackKnightsFortressProperties.FORTRESS_ENTRANCE

private object BlackKnightsFortressProperties {

	val PROPERTIES = quest {
		info(
			name = "Black Knight's Fortress",
			description = "",
			startRegion = GameRegion.FALADOR,
			startLocation = SIR_AMIK_VARZE.location,
			questValue = VarpID.QUEST_BLACK_KNIGHTS_FORTRESS.id,
			completionValue = 20
		)
		reward(
			questPoints = 3
		)
		items {
			inventory(Def.Items.GENERAL.CABBAGE)
			equipment(Def.Items.EQUIPMENT.BRONZE_MED_HELM)
			equipment(Def.Items.EQUIPMENT.IRON_CHAINBODY)
			teleport(Def.Items.TELEPORT.FALADOR_TELEPORT_TAB, quantity = 1, withdraw = 5)
			bank(Def.Items.CONSUMABLE.LOBSTER, quantity = 1, withdraw = 20)
		}
	}

	val FORTRESS_ENTRANCE = Coordinate(3016, 3514, 0)

}

class BlackKnightsFortress(bot: Bot) : QuestDetail(bot, BlackKnightsFortressProperties.PROPERTIES) {
	override val log = getLogger("BlackKnightsFortress")
	override val steps = questSteps {
		step(0) {
			conversation {
				name { "Navigate to Sir Amik Varze" }
				entry { SIR_AMIK_VARZE.isVisibleReachable() }
				with { SIR_AMIK_VARZE.name }
				options {
					+"I seek a quest!"
					+"I laugh in the face of danger!"
					+"Yes."
				}
			}
			navigate {
				name { "Navigate to Sir Amik Varze" }
				location { SIR_AMIK_VARZE.location }
			}
		}
		step(1) {
			items {
				listOf(
				RequiredItem(Def.Items.GENERAL.CABBAGE, 1),
				RequiredItem(Def.Items.EQUIPMENT.BRONZE_MED_HELM, 1, storage = RequiredItem.StorageType.EQUIPPED),
				RequiredItem(Def.Items.EQUIPMENT.IRON_CHAINBODY, 1, storage = RequiredItem.StorageType.EQUIPPED),
				RequiredItem(Def.Items.CONSUMABLE.LOBSTER, 1, withdraw = 20)
				)
			}
			operation {
				entry = { RuneScape.isCutscenePlaying() }
				action = { Delay.until({ !RuneScape.isCutscenePlaying() }) }
			}
			operation {
				entry = { ChatDialog.isOpen() }
				action = {
					ChatDialog.getContinue()?.select()
					Execution.delay(600)
				}
			}
			interact {
				name { "Listen-at grill" }
				action { "Listen-at" }
				with { sceneObj("Grill", on = Coordinate(3026, 3507, 0)) }
			}
			interact {
				name { "Climb-down ladder 3" }
				action { "Climb-down" }
				with { sceneObj("Ladder", on = Coordinate(3021, 3510, 1)) }
			}
			interact {
				name { "Open door" }
				action { "Open" }
				with { sceneObj("Sturdy door", on = Coordinate(3025, 3511, 1)) }
			}
			interact {
				name { "Climb-down ladder 2" }
				action { "Climb-down" }
				with { sceneObj("Ladder", on = Coordinate(3025, 3513, 2)) }
			}
			interact {
				name { "Climb-up ladder 3" }
				action { "Climb-up" }
				with { sceneObj("Ladder", on = Coordinate(3023, 3513, 1)) }
			}
			interact {
				name { "Open door" }
				action { "Open" }
				with { sceneObj("Door", on = Coordinate(3019, 3515, 1)) }
			}
			interact {
				name { "Climb-down ladder 1" }
				action { "Climb-down" }
				with { sceneObj("Ladder", on = Coordinate(3017, 3516, 2)) }
			}
			interact {
				name { "Climb-up ladder 2" }
				action { "Climb-up" }
				with { sceneObj("Ladder", on = Coordinate(3016, 3519, 1)) }
			}
			interact {
				name { "Climb-up ladder 1" }
				entry { Coordinate(3015, 3518, 0).isReachable }
				action { "Climb-up" }
				with { sceneObj("Ladder") }
			}
			interact {
				name { "Push wall" }
				entry { Coordinate(3016, 3515, 0).isReachable }
				action { "Push" }
				with { sceneObj("Wall", addEntryCondition = false) }
			}
			interact {
				name { "Enter Black Knights Fortress" }
				entry { FORTRESS_ENTRANCE.distanceToPlayer()?.let { it < 10 } ?: false }
				action { "Open" }
				with { sceneObj("Sturdy door") }
			}
			navigate {
				name { "Navigate to the Black Knight's Fortress" }
				location { FORTRESS_ENTRANCE }
			}
		}
		step(2) {
			operation {
				entry = { ChatDialog.isOpen() }
				action = {
					ChatDialog.getOption("I don't care. I'm going in anyway.").let {
						it?.select()
						Execution.delay(100)
					}
					ChatDialog.getContinue()?.select()
					Execution.delay(100)
				}
			}
			operation {
				entry = { RuneScape.isCutscenePlaying() }
				action = { Delay.until({ !RuneScape.isCutscenePlaying() }) }
			}
			chapter {
				entry { Coordinate(3030, 3507, 1).isReachable }
				interact {
					name { "Use cabbage on hole" }
					entry { Inventory.isItemSelected() }
					action { "Use" }
					with { sceneObj("Hole", on = Coordinate(3031, 3507, 1)) }
				}
				interact {
					name { "Use cabbage" }
					entry { Inventory.contains(Def.Items.GENERAL.CABBAGE.name) }
					action { "Use" }
					with { invItem(Def.Items.GENERAL.CABBAGE.name) }
				}
			}
			interact {
				name { "Climb-up ladder" }
				action { "Climb-up" }
				with { sceneObj("Ladder", on = Coordinate(3022, 3518, 0)) }
			}
			interact {
				name { "Push wall" }
				action { "Push" }
				with { sceneObj("Wall", on = Coordinate(3030, 3510, 1)) }
			}
			interact {
				name { "Open door" }
				action { "Open" }
				with { sceneObj("Door", on = Coordinate(3020, 3515, 0)) }
			}
			interact {
				name { "Push wall" }
				action { "Push" }
				with { sceneObj("Wall", on = Coordinate(3016, 3517, 0)) }
			}
			interact {
				name { "Climb-down ladder" }
				action { "Climb-down" }
				with { sceneObj("Ladder", on = Coordinate(3015, 3519, 1)) }
			}
			interact {
				name { "Climb-down ladder" }
				action { "Climb-down" }
				with { sceneObj("Ladder", on = Coordinate(3016, 3519, 2)) }
			}
			interact {
				name { "Climb-up ladder" }
				action { "Climb-up" }
				with { sceneObj("Ladder", on = Coordinate(3017, 3516, 1)) }
			}
			interact {
				name { "Open door" }
				action { "Open" }
				with { sceneObj("Door", on = Coordinate(3019, 3515, 1)) }
			}
			interact {
				name { "Climb-down ladder" }
				action { "Climb-down" }
				with { sceneObj("Ladder", on = Coordinate(3023, 3513, 2)) }
			}
			interact {
				name { "Climb-up ladder" }
				action { "Climb-up" }
				with { sceneObj("Ladder", on = Coordinate(3025, 3513, 1)) }
			}
			interact {
				name { "Open sturdy door" }
				action { "Open" }
				with { sceneObj("Sturdy door", on = Coordinate(3025, 3511, 1)) }
			}
			interact {
				name { "Climb-up ladder" }
				action { "Climb-up" }
				with { sceneObj("Ladder", on = Coordinate(3021, 3510, 0)) }
			}
		}
		step(3) {
			chapter {
				entry { Players.getLocal()?.position?.let { it.plane == 0 } ?: false }
				interact {
					name { "Open sturdy door" }
					entry { Coordinate(3016, 3515, 0).isReachable }
					action { "Open" }
					with { sceneObj("Sturdy door", on = Coordinate(3016, 3514, 0)) }
				}
				conversation {
					name { "Navigate to Sir Amik Varze" }
					entry { SIR_AMIK_VARZE.isVisibleReachable() }
					with { SIR_AMIK_VARZE.name }
				}
				navigate {
					name { "Navigate to Sir Amik Varze" }
					location { SIR_AMIK_VARZE.location }
				}
			}
			interact {
				name { "Open door" }
				action { "Open" }
				with { sceneObj("Door", on = Coordinate(3020, 3515, 0)) }
			}
			interact {
				name { "Climb-down ladder" }
				action { "Climb-down" }
				with { sceneObj("Ladder", on = Coordinate(3022, 3518, 1)) }
			}
			interact {
				name { "Push wall" }
				action { "Push" }
				with { sceneObj("Wall", on = Coordinate(3030, 3510, 1)) }
			}
		}
	}

	override fun atStartLocation(): Boolean {
		return SIR_AMIK_VARZE.isVisibleReachable()
	}
}
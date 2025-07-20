package com.runemate.rebirther.quest.members

import com.runemate.corev2.extensions.containsPlayer
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.local.Quest
import com.runemate.game.api.hybrid.local.Skill
import com.runemate.game.api.hybrid.local.VarpID
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.hybrid.region.Players
import com.runemate.rebirther.Bot
import com.runemate.rebirther.details.Def.Items
import com.runemate.rebirther.details.npc.NPCDatabase
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.items
import com.runemate.rebirther.quest.builder.quest
import com.runemate.rebirther.quest.builder.questSteps
import com.runemate.rebirther.quest.builder.reward
import com.runemate.rebirther.quest.members.DruidicRitualProperties.CAULDRON_OF_THUNDER
import com.runemate.rebirther.quest.members.DruidicRitualProperties.GATE_ENTRANCE
import com.runemate.rebirther.quest.members.DruidicRitualProperties.KAQEMEEX
import com.runemate.rebirther.quest.members.DruidicRitualProperties.SANFEW
import com.runemate.rebirther.quest.members.DruidicRitualProperties.exitDoorInteraction
import com.runemate.rebirther.util.ObjectInteraction
import com.runemate.rebirther.util.ObjectUtil


private object DruidicRitualProperties {
	val KAQEMEEX = NPCDatabase.QuestNPCDetail(
		name = "Kaqemeex",
		ids = setOf(8337, 5045),
		gameRegion = GameRegion.TAVERLEY,
		location = GameRegion.TAVERLEY.DRUIDS_CIRCLE,
		quests = setOf(Quest.OSRS.DRUIDIC_RITUAL)
	)

	val SANFEW = NPCDatabase.QuestNPCDetail(
		name = "Sanfew",
		ids = setOf(12483, 5044),
		gameRegion = GameRegion.TAVERLEY,
		location = Coordinate(2898, 3427, 1).area,
		quests = setOf(Quest.OSRS.DRUIDIC_RITUAL)
	)

	val exitDoorInteraction = ObjectInteraction(
		name = "Prison door",
		action = "Open",
		coordinate = Coordinate(2889, 9830, 0),
	)

	val PROPERTIES = quest {
		name = "Druidic Ritual"
		description = ""
		startRegion = GameRegion.TAVERLEY
		startLocation = GameRegion.TAVERLEY.DRUIDS_CIRCLE
		questValue = VarpID.QUEST_DRUIDIC_RITUAL.id
		completionValue = 4

		reward(
			questPoints = 4,
			experience = mapOf(Skill.HERBLORE to 250)
		)

		items {
			inventory(Items.INGREDIENT.RAW_BEAR_MEAT)
			inventory(Items.INGREDIENT.RAW_RAT_MEAT)
			inventory(Items.INGREDIENT.RAW_BEEF)
			inventory(Items.INGREDIENT.RAW_CHICKEN)
		}
	}

	val GATE_ENTRANCE = Coordinate(2888, 9830, 0)
	val CAULDRON_OF_THUNDER = "Cauldron of Thunder"

}

class DruidicRitual(bot: Bot) : QuestDetail(bot, DruidicRitualProperties.PROPERTIES) {
	override val log = getLogger("DruidicRitual")

	override val steps = questSteps {
		step(0) {
			conversation {
				name { "Talk to Kaqemeex" }
				entry { KAQEMEEX.isVisibleReachable() }
				with { KAQEMEEX.name }
				options {
					+"Who are you?"
					+"What about the stone circle full of dark wizards?"
					+"Yes."
				}
			}
			navigate {
				name { "Navigate to Kaqemeex" }
				location { GameRegion.TAVERLEY.DRUIDS_CIRCLE }
			}
		}
		step(1) {
			conversation {
				name { "Talk to Sanfew" }
				entry { SANFEW.isVisibleReachable() }
				with { SANFEW.name }
				options {
					+"I've been sent to help purify the Varrock stone circle."
					+"Ok, I'll do that then."
				}
			}
			navigate {
				name { "Navigate to Sanfew" }
				location { SANFEW.location }
			}
		}
		step(2) {
			chapter {
				name { "Deliver Enchanted Meat" }
				entry {
					Inventory.containsAllOf(
						"Enchanted Bear",
						"Enchanted rat",
						"Enchanted beef",
						"Enchanted chicken"
					)
				}
				conversation {
					name { "Talk to Sanfew" }
					entry { SANFEW.isVisibleReachable() }
					with { SANFEW.name }
				}
				navigate {
					name { "Navigate to Sanfew" }
					location { SANFEW.location }
				}
			}
			chapter {
				name { "Cauldron" }
				entry { ObjectUtil.isVisibleReachable(CAULDRON_OF_THUNDER) }
				interact {
					name { "Use Cauldron of Thunder" }
					entry { Inventory.isItemSelected() }
					action { "Use" }
					with { sceneObj(CAULDRON_OF_THUNDER, on = Coordinate(2893, 9831, 0)) }
				}
				interact {
					name { "Use Raw Chicken" }
					action { "Use" }
					with { invItem(Items.INGREDIENT.RAW_CHICKEN.name) }
				}
				interact {
					name { "Use Raw Beef" }
					action { "Use" }
					with { invItem(Items.INGREDIENT.RAW_BEEF.name) }
				}
				interact {
					name { "Use Raw Rat Meat" }
					action { "Use" }
					with { invItem(Items.INGREDIENT.RAW_RAT_MEAT.name) }
				}
				interact {
					name { "Use Raw Bear Meat" }
					action { "Use" }
					with { invItem(Items.INGREDIENT.RAW_BEAR_MEAT.name) }
				}
			}
			interact {
				name { "Open the prison door" }
				entry {
					Players.getLocal()?.position?.let { it.x <= 2888 } ?: false
							&& ObjectUtil.checkObjectHasAction(exitDoorInteraction)
				}
				action { "Open" }
				with { sceneObj("Prison door") }
			}
			navigate {
				name { "Navigate to gate entrance" }
				location { GATE_ENTRANCE }
			}
		}
		step(3) {
			conversation {
				name { "Talk to Kaqemeex" }
				entry { KAQEMEEX.isVisibleReachable() }
				with { KAQEMEEX.name }
			}
			navigate {
				name { "Navigate to Kaqemeex" }
				location { GameRegion.TAVERLEY.DRUIDS_CIRCLE }
			}
		}
	}

	override fun atStartLocation(): Boolean = GameRegion.TAVERLEY.DRUIDS_CIRCLE.containsPlayer()
}
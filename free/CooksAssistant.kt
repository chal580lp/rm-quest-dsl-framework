package com.runemate.rebirther.quest.free

import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.entities.Npc
import com.runemate.game.api.hybrid.local.Skill
import com.runemate.game.api.hybrid.local.hud.interfaces.ChatDialog
import com.runemate.rebirther.Bot
import com.runemate.rebirther.details.Def.Items
import com.runemate.rebirther.details.npc.type.QuestNPC
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.*


private object CooksAssistantProperties {
	val properties = quest {
		info(
			name = "Cook's Assistant",
			description = "",
			startRegion = GameRegion.LUMBRIDGE,
			startLocation = QuestNPC.COOK.location,
			questValue = 29,
			completionValue = 2
		)
		reward(
			questPoints = 1,
			experience = mapOf(Skill.COOKING to 300)
		)
		npcs {
			friendly(QuestNPC.COOK)
		}

		items {
			inventory(Items.INGREDIENT.EGG)
			inventory(Items.INGREDIENT.POT_OF_FLOUR)
			inventory(Items.INGREDIENT.BUCKET_OF_MILK)
			teleport(Items.TELEPORT.LUMBRIDGE_TELEPORT_TAB, quantity = 1, withdraw = 5)
			teleport(Items.TELEPORT.RING_OF_WEALTH)
		}
	}
}

class CooksAssistant(bot: Bot) : QuestDetail(bot, CooksAssistantProperties.properties) {
	override val log = getLogger("CooksAssistant")

	private val cook: Npc?
		get() = QuestNPC.COOK.get()

	override val steps = questSteps {
		step(0) {
			name { "Start the quest" }
			entry { cook != null && cook?.isVisible == true }

			conversation {
				name { "Talk to Cook" }
				with { QuestNPC.COOK.name }
				options {
					+"What's wrong?"
					+"Yes."
				}
			}

			fallback { traverseToCook() }
		}

		step(1) {
			name = "Gather ingredients and return to cook"

			operation {
				name { "Return to the cook" }
				entry {
					ChatDialog.isOpen() ||
							(Items.INGREDIENT.EGG.inInventory() &&
									Items.INGREDIENT.POT_OF_FLOUR.inInventory() &&
									Items.INGREDIENT.BUCKET_OF_MILK.inInventory())
				}
				action {
					QuestNPC.COOK.talkTo("I have everything you need for your cake.")
				}
			}
		}

		step(2) {
			name = "Finish the quest"

			conversation {
				name { "Talk to Cook" }
				with { QuestNPC.COOK.name }
			}
		}
	}

	override fun navigateToStartLocation() {
		traverseToCook()
	}

	override fun atStartLocation(): Boolean {
		return QuestNPC.COOK.isVisibleReachable()
	}

	private fun traverseToCook() {
		log.debug("Traversing to the Cook")
		if (!QuestNPC.COOK.gameRegion.isPlayerInRegion()) {
			Navigation.toRegion(QuestNPC.COOK.gameRegion)
		} else {
			Navigation.toArea(QuestNPC.COOK.location)
		}
	}
}
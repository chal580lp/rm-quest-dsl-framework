package com.runemate.rebirther.quest.members

import com.runemate.corev2.extensions.chop
import com.runemate.corev2.extensions.containsPlayer
import com.runemate.corev2.extensions.getTargetingNpcs
import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.corev2.utility.SimpleTimer
import com.runemate.game.api.hybrid.local.Camera
import com.runemate.game.api.hybrid.local.Quest
import com.runemate.game.api.hybrid.local.Skill
import com.runemate.game.api.hybrid.local.VarpID
import com.runemate.game.api.hybrid.local.hud.interfaces.ChatDialog
import com.runemate.game.api.hybrid.local.hud.interfaces.Chatbox
import com.runemate.game.api.hybrid.local.hud.interfaces.Equipment
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory
import com.runemate.game.api.hybrid.location.Area.Rectangular
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.hybrid.region.GameObjects
import com.runemate.game.api.hybrid.region.GroundItems
import com.runemate.game.api.hybrid.region.Players
import com.runemate.game.api.hybrid.util.Regex
import com.runemate.game.api.osrs.local.hud.interfaces.Magic
import com.runemate.game.api.script.framework.listeners.ChatboxListener
import com.runemate.game.api.script.framework.listeners.VarbitListener
import com.runemate.game.api.script.framework.listeners.events.MessageEvent
import com.runemate.game.api.script.framework.listeners.events.VarbitEvent
import com.runemate.rebirther.Bot
import com.runemate.rebirther.baseclasses.RequiredItem.StorageType.EQUIPPED
import com.runemate.rebirther.details.Def.Items
import com.runemate.rebirther.details.npc.NPCDatabase.MonsterNPCDetail
import com.runemate.rebirther.details.npc.NPCDatabase.QuestNPCDetail
import com.runemate.rebirther.details.shop.region.ArdougneShops
import com.runemate.rebirther.handler.ConsumablesHandler
import com.runemate.rebirther.handler.NpcInteractionHandler.Companion.talkToNPC
import com.runemate.rebirther.handler.SafeSpotHandler
import com.runemate.rebirther.handler.StoreHandler
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.info
import com.runemate.rebirther.quest.builder.items
import com.runemate.rebirther.quest.builder.quest
import com.runemate.rebirther.quest.builder.reward
import com.runemate.rebirther.quest.framework.Operation
import com.runemate.rebirther.quest.framework.QuestStep
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.COMMANDER_MONTAI
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.GNOME_TWO_BUILDING
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.KHAZARD_WARLORD
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.KHAZARD_WARLORD_MONSTER
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.KING_BOLREN
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.TRACKER_GNOME_1
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.TRACKER_GNOME_2
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.TRACKER_GNOME_3
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.WARLORD_SAFE_AREA
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.ballistaInteraction
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.closedChestInteraction
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.crumbledWallInteraction
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.elkoyInteraction
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.exitDoorInteraction
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.ladderDownInteraction
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.ladderUpInteraction
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.looseRailingInteraction
import com.runemate.rebirther.quest.members.TreeGnomeVillageProperties.openChestInteraction
import com.runemate.rebirther.util.InteractionUtil.interactOrWalkTo
import com.runemate.rebirther.util.InteractionUtil.interactWithCloseObjects
import com.runemate.rebirther.util.NpcInteraction
import com.runemate.rebirther.util.ObjectInteraction
import com.runemate.rebirther.util.QuestUtil.requiredItem
import com.runemate.rebirther.util.QuestUtil.requiredItems
import com.runemate.rebirther.util.SpellCaster

private object TreeGnomeVillageProperties {
	//6266 ELKOY at village skip have to squeeze through if in village
	//05:29:47[5]  MenuInt | ID: 2783 | Action: Talk-to | Target: Elkoy | TargetType: NPC | TargetEntity: Elkoy(level: 0, position: 2514, 3158, 0) | Elkoy | ID: 6266 | Coordinate(2514, 3158, 0) | Reachable: false | Surrounding: false
	//05:30:26[23]  MenuInt | ID: 2186 | Action: Squeeze-through | Target: Loose Railing | TargetType: GAME_OBJECT | TargetEntity: Loose Railing [2515, 3161, 0] | Coordinate(2515, 3161, 0) | [Squeeze-through] | Reachable: true | Surrounding: true
	val KING_BOLREN = QuestNPCDetail(
		name = "King Bolren",
		ids = setOf(4963),
		gameRegion = GameRegion.TREE_GNOME_VILLAGE,
		location = Coordinate(2540, 3169, 0).area,
		quests = setOf(Quest.OSRS.TREE_GNOME_VILLAGE)
	)

	val COMMANDER_MONTAI = QuestNPCDetail(
		name = "Commander Montai",
		ids = setOf(4964),
		gameRegion = GameRegion.TREE_GNOME_VILLAGE,
		location = Coordinate(2523, 3209, 0).area,
		quests = setOf(Quest.OSRS.TREE_GNOME_VILLAGE)
	)

	val TRACKER_GNOME_1 = QuestNPCDetail(
		name = "Tracker gnome 1",
		ids = setOf(4975),
		gameRegion = GameRegion.TREE_GNOME_VILLAGE,
		location = Coordinate(2503, 3260, 0).area,
		quests = setOf(Quest.OSRS.TREE_GNOME_VILLAGE)
	)

	val TRACKER_GNOME_2 = QuestNPCDetail(
		name = "Tracker gnome 2",
		ids = setOf(4976),
		gameRegion = GameRegion.TREE_GNOME_VILLAGE,
		location = Coordinate(2523, 3256, 0).area,
		quests = setOf(Quest.OSRS.TREE_GNOME_VILLAGE),
		reachable = false
	)

	val TRACKER_GNOME_3 = QuestNPCDetail(
		name = "Tracker gnome 3",
		ids = setOf(4977),
		gameRegion = GameRegion.TREE_GNOME_VILLAGE,
		location = Coordinate(2493, 3235, 0).area,
		quests = setOf(Quest.OSRS.TREE_GNOME_VILLAGE)
	)

	val KHAZARD_WARLORD = QuestNPCDetail(
		name = "Khazard warlord",
		ids = setOf(4971),
		gameRegion = GameRegion.UNMAPPED,
		location = Coordinate(2457, 3302, 0).area,
		quests = setOf(Quest.OSRS.TREE_GNOME_VILLAGE)
	)

	val KHAZARD_WARLORD_MONSTER = MonsterNPCDetail(
		name = "Khazard Warlord",
		ids = setOf(7622),
		gameRegion = GameRegion.UNMAPPED,
		location = Coordinate(2457, 3302, 0).area,
		combatLevel = 112
	)


	val ballistaInteraction = ObjectInteraction(
		name = "Ballista",
		action = "Fire",
		coordinate = Coordinate(2508, 3210, 0)
	)

	val looseRailingInteraction = ObjectInteraction(
		name = "Loose Railing",
		action = "Squeeze-through",
		coordinate = Coordinate(2515, 3161, 0)
	)

	val elkoyInteraction = NpcInteraction(
		name = "Elkoy",
		action = "Follow",
		coordinate = Coordinate(2516, 3159, 0)
	)

	val crumbledWallInteraction = ObjectInteraction(
		name = "Crumbled wall",
		action = "Climb-over",
		coordinate = Coordinate(2509, 3253, 0),
		startCoordinate = Coordinate(2509, 3252, 0)
	)

	val ladderUpInteraction = ObjectInteraction(
		name = "Ladder",
		action = "Climb-up",
		coordinate = Coordinate(2503, 3252, 0)
	)

	val closedChestInteraction = ObjectInteraction(
		name = "Closed chest",
		action = "Open",
		coordinate = Coordinate(2506, 3259, 1)
	)

	val openChestInteraction = ObjectInteraction(
		name = "Open chest",
		action = "Search",
		coordinate = Coordinate(2506, 3259, 1)
	)

	val ladderDownInteraction = ObjectInteraction(
		name = "Ladder",
		action = "Climb-down",
		coordinate = Coordinate(2503, 3252, 1)
	)

	val exitDoorInteraction = ObjectInteraction(
		name = "Door",
		action = "Open",
		coordinate = Coordinate(2502, 3250, 0),
		startCoordinate = Coordinate(2502, 3251, 0)
	)

	val GNOME_TWO_BUILDING = Rectangular(Coordinate(2525, 3255, 0), Coordinate(2522, 3256, 0))
	val WARLORD_SAFE_AREA = Rectangular(Coordinate(2441, 3304, 0), Coordinate(2437, 3299, 0))

	val PROPERTIES = quest {
		info(
			name = "Tree Gnome Village",
			description = "Help the tree gnomes defend their village and recover the orbs of protection.",
			startRegion = GameRegion.TREE_GNOME_VILLAGE,
			startLocation = KING_BOLREN.location,
			questValue = VarpID.QUEST_TREE_GNOME_VILLAGE.id,
			completionValue = 9,
			beta = true
		)
		reward(
			questPoints = 2,
			experience = mapOf(Skill.ATTACK to 11450),
		)
		items {
			teleport(Items.TELEPORT.RING_OF_DUELING)
		}
	}

}

class TreeGnomeVillage(bot: Bot) : QuestDetail(bot, TreeGnomeVillageProperties.PROPERTIES), VarbitListener,
	ChatboxListener {
	override val log = getLogger("TreeGnomeVillage")
	lateinit var consumablesHandler: ConsumablesHandler
	private val storeHandler = StoreHandler(bot)


	//? starting gnome spy
	private var gnome: Int = 2
	private var ballistaCode: String = ""
	private var warlordRetreat = false
	private val timer = SimpleTimer()


	override fun onStart() {
		consumablesHandler = ConsumablesHandler(
			minEatAt = 9,
			maxEatAtPercentage = 50,
			minHealthPercentage = 50,
		)
		bot.eventDispatcher.addListener(consumablesHandler)
		bot.eventDispatcher.addListener(this)
		log.info("Starting Tree Gnome Village quest")
	}

	override fun onFinish() {
		bot.eventDispatcher.removeListener(consumablesHandler)
		bot.eventDispatcher.removeListener(this)
		log.info("Tree Gnome Village quest completed")
	}

	override fun navigateToStartLocation() {
		Navigation.toLocationInRegion(PROPERTIES.startLocation, PROPERTIES.startRegion)
	}

	override fun atStartLocation(): Boolean {
		return KING_BOLREN.get() != null
	}

	fun exitVillage() {
		log.debug("Exiting village")
		if (GameRegion.TREE_GNOME_VILLAGE.INNER_VILLAGE.any { it.containsPlayer() }) {

			interactOrWalkTo(elkoyInteraction, looseRailingInteraction)
		}
	}

	override val steps: List<QuestStep> = listOf(
		QuestStep(
			name = "Starting off",
			value = 0,
			entry = { GameRegion.TREE_GNOME_VILLAGE.isPlayerInRegion() },
			fallback = { navigateToStartLocation() },
			operations = listOf(
				Operation(
					name = "Talk to King Bolren",
					entry = { KING_BOLREN.isReachable() },
					action = {
						KING_BOLREN.talkTo("Yes.", "Can I help at all?")
					},
					fallback = { navigateToStartLocation() }
				),
			)
		),
		QuestStep(
			value = 1,
			entry = { GameRegion.TREE_GNOME_VILLAGE.isPlayerInRegion() },
			fallback = { navigateToStartLocation() },
			operations = listOf(
				Operation(
					name = "Talk to Commander Montai",
					entry = { COMMANDER_MONTAI.isVisibleReachable() && !Inventory.contains("Logs") },
					action = {
						COMMANDER_MONTAI.talkTo("Ok, I'll gather some wood.")
					},
					fallback = { Navigation.toArea(COMMANDER_MONTAI.location) }
				),
				Operation(
					entry = { GameRegion.TREE_GNOME_VILLAGE.INNER_VILLAGE.any { it.containsPlayer() } },
					action = {
						exitVillage()
					}
				)
			)
		),
		QuestStep(
			name = "Fetch and hand in logs",
			value = 2,
			entry = {
				Inventory.contains(Regex.getPatternForContainsString("axe")) || Equipment.contains(
					Regex.getPatternForContainsString(
						"axe"
					)
				)
			},
			fallback = {
				storeHandler.buyFromStore(
					Items.SKILL_TOOL.BRONZE_AXE,
					1,
					ArdougneShops.PORT_KHAZARD_GENERAL_STORE
				)
			},
			operations = listOf(
				Operation(
					name = "Give logs to Commander Montai",
					entry = { COMMANDER_MONTAI.get() != null && Inventory.getQuantity("Logs") >= 6 },
					action = {
						talkToNPC(COMMANDER_MONTAI.get(), listOf())
					}
				),
				Operation(
					name = "Traverse to Commander Montai",
					entry = { Inventory.getQuantity("Logs") >= 6 },
					action = {
						Navigation.toArea(COMMANDER_MONTAI.location)
					}
				),
				Operation(
					name = "Cut logs",
					entry = { GameRegion.TREE_GNOME_VILLAGE.isPlayerInRegion() },
					fallback = { Navigation.toRegion(GameRegion.TREE_GNOME_VILLAGE) },
					action = {
						val tree =
							GameObjects.newQuery().names("Tree").actions("Chop down").surroundingsReachable().results()
								.nearest()
						tree.let {
							it?.chop()
						}
					}
				)
			)
		),
		QuestStep(
			name = "Talk to Montai again",
			value = 3,
			entry = { COMMANDER_MONTAI.isVisibleReachable() },
			fallback = { Navigation.toArea(COMMANDER_MONTAI.location) },
			operation = Operation(
				action = {
					COMMANDER_MONTAI.talkTo("I'll try my best.")
				}
			)
		),
		QuestStep(
			name = "The three trackers",
			value = 4,
			operations = listOf(
				Operation(
					name = "Fire the ballista",
					entry = { ballistaCode != "" },
					action = {
						if (ChatDialog.isOpen()) {
							if (ChatDialog.getOptions().isNotEmpty()) {
								log.debug("OPTIONS: {}", ChatDialog.getOptions())
								ChatDialog.getOption(ballistaCode)?.select()
							} else {
								ChatDialog.getContinue()?.select()
							}
							Delay.delayTicks(2)
						} else {
							interactOrWalkTo(
								ballistaInteraction,
								false
							)
						}
					},
				),
				Operation(
					name = "Talk to Tracker Gnome 3",
					entry = { gnome == 3 },
					action = {
						if (!TRACKER_GNOME_3.isVisibleReachable()) {
							Navigation.toArea(TRACKER_GNOME_3.location)
						} else {
							TRACKER_GNOME_3.talkTo()
						}
					},
				),
				Operation(
					name = "Talk to Tracker Gnome 1",
					entry = { gnome == 1 },
					action = {
						if (!TRACKER_GNOME_1.isVisibleReachable()) {
							Navigation.toArea(TRACKER_GNOME_1.location)
						} else {
							TRACKER_GNOME_1.talkTo()
						}
					},
				),
				Operation(
					name = "Talk to Tracker Gnome 2",
					entry = {
						gnome == 2 && GNOME_TWO_BUILDING.containsPlayer()
					},
					action = {
						TRACKER_GNOME_2.talkTo()
					},
					fallback = { Navigation.viaWeb(TRACKER_GNOME_2.location) }
				)

			)
		),
		QuestStep(
			name = "The orbs",
			value = 5,
			//condition = { progress >= 20 },
			operations = listOf(
				Operation(
					name = "Collect orb of protection",
					entry = { GameRegion.TREE_GNOME_VILLAGE.isPlayerInRegion() },
					action = {
						interactOrWalkTo(
							openChestInteraction, closedChestInteraction,
							ladderUpInteraction, crumbledWallInteraction
						)
					},
					fallback = { navigateToStartLocation() }
				)
			)
		),
		QuestStep(
			name = "The orbs",
			value = 6,
			entry = { Inventory.contains("Orb of protection") },
			operations = listOf(
				Operation(
					entry = { KING_BOLREN.isVisibleReachable() },
					action = {
						KING_BOLREN.talkTo("I will find the warlord and bring back the orbs.")
					},
				),
				Operation(
					name = "Return to King Bolren",
					entry = { exitDoorInteraction.hasPassed() },
					action = { Navigation.viaSceneToEnh(KING_BOLREN.location) },
				),
				Operation(
					name = "Leave house",
					entry = { GameRegion.TREE_GNOME_VILLAGE.isPlayerInRegion() },
					action = { interactWithCloseObjects(exitDoorInteraction, ladderDownInteraction) },
					fallback = { navigateToStartLocation() }
				)
			)
		),
		QuestStep(
			name = "Kill warlord",
			value = 7,
			items = requiredItems(
				requiredItem(Items.EQUIPMENT.STAFF_OF_AIR, 1, storage = EQUIPPED),
				requiredItem(Items.MAGIC.MIND_RUNE, 300, 1000),
				requiredItem(Items.CONSUMABLE.LOBSTER, 1, withdraw = 20)
			),
			operations = listOf(
				Operation(
					name = "Loot orbs of protection",
					entry = {
						GroundItems.newQuery().names("Orbs of protection").reachable().results().isNotEmpty()
					},
					action = {
						GroundItems.newQuery().names("Orbs of protection").reachable().results().nearest()?.let {
							if (!it.isVisible) Camera.turnTo(it)
							it.take()
							Delay.untilInventoryContains("Orbs of protection")
						}
					}
				),
				Operation(
					entry = { warlordRetreat },
					action = { runFromTroopers() }
				),
				Operation(
					name = "Handle killing Khazard Warlord",
					entry = { KHAZARD_WARLORD_MONSTER.isValid() },
					action = { handleWarlord() }
				),
				Operation(
					name = "Talk to Khazard Warlord",
					entry = { KHAZARD_WARLORD.isVisibleReachable() },
					action = {
						if ((Players.getLocal()?.healthGauge?.isValid == true) && (Players.getLocal()
								?.getTargetingNpcs()?.any { it.definition?.name == "Khazard trooper" } == true)
						) {
							warlordRetreat = true
						} else {
							configureCombat()
							KHAZARD_WARLORD.talkTo()
						}
					},
					fallback = { Navigation.toArea(KHAZARD_WARLORD.location) }
				)
			)
		), QuestStep(
			name = "Finish quest",
			value = 8,
			entry = { true },
			operations = listOf(
				Operation.continueDialog,
				Operation(
					name = "Finish quest",
					entry = { KING_BOLREN.isVisibleReachable() },
					action = {
						KING_BOLREN.talkTo("Yes.")
					},
					fallback = { Navigation.viaEnhanced(KING_BOLREN.location) }
				),
				Operation(
					entry = { !Inventory.contains("Orbs of protection") },
					action = { repeatStep(7) }
				),

				)

		)
	)

	private fun handleWarlord() {
		if (!configureCombat()) return
		SafeSpotHandler.attackFrom(KHAZARD_WARLORD_MONSTER.get(), Coordinate(2444, 3297, 0))
	}

	private fun configureCombat(): Boolean {
		if (!Magic.WIND_STRIKE.isAutocasting) {
			SpellCaster.setAutocast(SpellCaster.Spell.WIND_STRIKE)
			return false
		}
		return true
	}

	private fun runFromTroopers() {
		if (warlordRetreat) {
			log.debug("Retreating from warlord")
			if (!WARLORD_SAFE_AREA.containsPlayer()) {
				log.debug("Retreating from warlord")
				Navigation.toArea(WARLORD_SAFE_AREA)
			} else {
				if (!timer.isRunning()) {
					timer.start(10000)
					log.debug("Timer started for 10 seconds")
				} else if (timer.isFinished()) {
					log.debug("Timer finished")
					warlordRetreat = false
				} else {
					log.debug("Waiting for timer to finish")
				}
			}
		}
	}

	override fun onValueChanged(event: VarbitEvent?) {
		if (event?.varbit?.id == 10670 && event.newValue == 1) {
			gnome = when (gnome) {
				2 -> 1
				1 -> 3
				else -> 3
			}
		}
	}

	override fun onMessageReceived(event: MessageEvent?) {
		if (event?.type == Chatbox.Message.Type.SERVER || event?.type == Chatbox.Message.Type.UNKNOWN) {
			val message = event.message
			when {
				message.contains("Less than my hands") -> ballistaCode = "0001"
				message.contains("More than my head, less than my fingers") -> ballistaCode = "0002"
				message.contains("More than we, less than our feet") -> ballistaCode = "0003"
				message.contains("My legs and your legs") -> ballistaCode = "0004"
			}
			if (ballistaCode != "") {
				log.debug("Ballista code: $ballistaCode")
			}
			if (event.speaker == "Tracker gnome 3") {
				log.debug("Gnome 3: ${event.message}")
			}
		}
	}
}

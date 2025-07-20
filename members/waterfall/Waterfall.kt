package com.runemate.rebirther.quest.members.waterfall

import com.runemate.corev2.extensions.containsPlayer
import com.runemate.corev2.extensions.distanceToPlayer
import com.runemate.corev2.extensions.isReachable
import com.runemate.corev2.extensions.useOn
import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil
import com.runemate.game.api.hybrid.RuneScape
import com.runemate.game.api.hybrid.entities.GameObject
import com.runemate.game.api.hybrid.local.Quest.OSRS.WATERFALL_QUEST
import com.runemate.game.api.hybrid.local.Skill
import com.runemate.game.api.hybrid.local.VarpID
import com.runemate.game.api.hybrid.local.hud.interfaces.Bank
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory
import com.runemate.game.api.hybrid.location.Area
import com.runemate.game.api.hybrid.location.Area.Rectangular
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.hybrid.region.GameObjects
import com.runemate.game.api.hybrid.region.Players
import com.runemate.game.api.script.Execution
import com.runemate.rebirther.Bot
import com.runemate.rebirther.baseclasses.RequiredItem
import com.runemate.rebirther.baseclasses.RequiredItem.StorageType
import com.runemate.rebirther.details.Def
import com.runemate.rebirther.details.Def.Items
import com.runemate.rebirther.details.gameobject.get
import com.runemate.rebirther.details.gameobject.isVisible
import com.runemate.rebirther.details.gameobject.type.QuestObject
import com.runemate.rebirther.details.item.ItemDatabase
import com.runemate.rebirther.details.npc.type.QuestNPC
import com.runemate.rebirther.handler.ConsumablesHandler
import com.runemate.rebirther.handler.NpcInteractionHandler
import com.runemate.rebirther.handler.TeleportHandler
import com.runemate.rebirther.handler.quest.PillarInteractionHandler
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.other.GameRegion.BARBARIAN_ASSAULT
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.*
import com.runemate.rebirther.quest.framework.Operation
import com.runemate.rebirther.quest.framework.QuestStep
import com.runemate.rebirther.util.ObjectInteraction
import com.runemate.rebirther.util.ObjectUtil
import com.runemate.rebirther.util.QuestUtil

private object WaterfallQuestProperties {
	val properties = quest {
		info(
			name = "Waterfall Quest",
			description = "",
			startRegion = BARBARIAN_ASSAULT,
			startLocation = QuestNPC.ALMERA.location,
			questValue = VarpID.QUEST_WATERFALL_QUEST.id,
			completionValue = 10,
			beta = true
		)
		reward(
			questPoints = 1,
			experience = mapOf(Skill.ATTACK to 13750, Skill.STRENGTH to 13750)
		)
		npcs {
			friendly(QuestNPC.ALMERA)
		}

		items {
			inventory(Items.UTILITY.ROPE, 1, 3)
			inventory(Items.CONSUMABLE.LOBSTER, 25, storage = StorageType.BANK)
			teleport(Items.TELEPORT.GAMES_NECKLACE)
			teleport(Items.TELEPORT.RING_OF_DUELING)
			teleport(Items.TELEPORT.SKILLS_NECKLACE)
			teleport(Items.TELEPORT.RING_OF_WEALTH)
		}
	}

	val island = Rectangular(Coordinate(2513, 3481, 0), Coordinate(2510, 3476, 0))
	val cliff = Rectangular(Coordinate(2513, 3469, 0), Coordinate(2511, 3464, 0))
	val hadleysUpstairs = Coordinate(2519, 3429, 1)
	val treeGnomeVillageDungeonEntrance = Rectangular(Coordinate(2540, 3155, 0), Coordinate(2534, 3155, 0))
	val crate = Coordinate(2548, 9566, 0)
	val prisonDoor = Coordinate(2515, 9575, 0)
	val glarialsTomb = Rectangular(Coordinate(2559, 9849, 0), Coordinate(2524, 9807, 0))
	val cliffEntrance = Coordinate(2511, 3463, 0)


	val rock: GameObject?
		get() = GameObjects.newQuery().names("Rock").on(Coordinate(2512, 3468, 0)).results().first()

	val castleWarsGnomeStrongholdArdougne = Area.Polygonal(
		Coordinate(2689, 3074, 0),
		Coordinate(2435, 3069, 0),
		Coordinate(2425, 3196, 0),
		Coordinate(2562, 3342, 0),
		Coordinate(2663, 3388, 0),
		Coordinate(2748, 3311, 0),
		Coordinate(2738, 3259, 0),
		Coordinate(2687, 3223, 0)
	)

	val WATERFALL_DUNGEON_AREA = Rectangular(Coordinate(2617, 9923, 0), Coordinate(2528, 9859, 0))
	val WATERFALL_DUNGEON_CRATE_ROOM = Rectangular(Coordinate(2582, 9888, 0), Coordinate(2595, 9874, 0))
	val WATERFALL_DUNGEON_GLARIALS_TOMB = Rectangular(Coordinate(2571, 9902, 0), Coordinate(2559, 9918, 0))

	val CRATE_ROOM_DOOR = ObjectInteraction("Large door", "Open", Coordinate(2582, 9876, 0))
	val GLARIAL_DOOR_THIRD = ObjectInteraction("Door", "Open", Coordinate(2566, 9901, 0))
	val GLARIAL_DOOR_SECOND = ObjectInteraction("Door", "Open", Coordinate(2568, 9893, 0))
	val GLARIAL_DOOR_FIRST = ObjectInteraction("Large door", "Open", Coordinate(2565, 9881, 0))

	val GLARIALS_PEBBLE = ItemDatabase.QuestItem(
		id = 294,
		name = "Glarial's pebble",
		tradeable = false,
		quests = setOf(WATERFALL_QUEST),
	)
	val GLARIALS_AMULET = ItemDatabase.QuestItem(
		id = 295,
		name = "Glarial's amulet",
		tradeable = false,
		quests = setOf(WATERFALL_QUEST),
	)
	val GLARIALS_URN = ItemDatabase.QuestItem(
		id = 296,
		name = "Glarial's urn",
		tradeable = false,
		quests = setOf(WATERFALL_QUEST),
	)

	val GLARIALS_ITEMS = listOf(
		GLARIALS_AMULET,
		GLARIALS_URN,
		GLARIALS_PEBBLE
	)

	val CRATE_KEY = ItemDatabase.QuestItem(
		id = 298,
		name = "Key",
		tradeable = false,
		quests = setOf(WATERFALL_QUEST),
	)
}

class Waterfall(bot: Bot) : QuestDetail(bot, WaterfallQuestProperties.properties) {
	override val log = LoggerUtil.getLogger("Waterfall")
	private lateinit var consumablesHandler: ConsumablesHandler
	private lateinit var pillarHandler: PillarInteractionHandler

	override fun onStart() {
		log.info("onStart ${PROPERTIES.name}")
		consumablesHandler = ConsumablesHandler(
			minEatAt = 9,
			maxEatAtPercentage = 50,
			minHealthPercentage = 50,
		)
		pillarHandler = PillarInteractionHandler()
		bot.eventDispatcher.addListener(consumablesHandler)
		bot.eventDispatcher.addListener(pillarHandler)
		log.info("Added 2 listeners: consumablesHandler, pillarHandler")
	}

	override fun onFinish() {
		log.info("onFinish ${PROPERTIES.name}")
		bot.eventDispatcher.removeListener(consumablesHandler)
		bot.eventDispatcher.removeListener(pillarHandler)
		log.info("Removed 2 listeners: consumablesHandler, pillarHandler")
	}


	override val steps: List<QuestStep> = listOf(
		QuestStep(
			name = "Talk to Almera to start the quest.",
			value = 0,
			items = listOf(QuestUtil.requiredItem(Items.UTILITY.ROPE, 1, 2)),
			operation = Operation(
				entry = { QuestNPC.ALMERA.isVisibleReachable() },
				action = { NpcInteractionHandler.talkToNPC("Almera", "Yes.") },
				fallback = { navigateToStartLocation() },
			)
		),
		QuestStep(
			name = "Speak to Hudon.",
			value = 1,
			items = listOf(QuestUtil.requiredItem(Items.UTILITY.ROPE, 1, 2)),
			operations = listOf(
				Operation(
					entry = { WaterfallQuestProperties.island.containsPlayer() },
					action = { NpcInteractionHandler.talkToNPC("Hudon") },
				),
				Operation(
					entry = { !WaterfallQuestProperties.island.containsPlayer() },
					action = { boardRaft() },
					fallback = { navigateToStartLocation() }
				),
			)
		),
		QuestStep(
			name = "Climb the cliff.",
			value = 2,
			items = listOf(QuestUtil.requiredItem(Items.UTILITY.ROPE, 1, 3)),
			operations = listOf(
				Operation(
					entry = { Inventory.contains("Book on baxtorian") },
					action = { Inventory.getItems("Rope").first()?.interact("Read") },
				),
				Operation(
					entry = { WaterfallQuestProperties.hadleysUpstairs.isReachable },
					action = { ObjectUtil.interactWithObjects("Bookcase", "Search") },
				),
				Operation(
					entry = { WaterfallQuestProperties.cliff.containsPlayer() },
					action = { Inventory.getItems("Rope").first()?.useOn("Dead tree") },
				),
				Operation(
					entry = { WaterfallQuestProperties.island.containsPlayer() && Items.UTILITY.ROPE.inInventory() },
					action = { Inventory.getItems("Rope").first()?.useOn(WaterfallQuestProperties.rock) }
				),
				Operation(
					entry = { Players.getLocal()?.position?.plane == 0 },
					action = {
						Navigation.toLocationInRegion(
							WaterfallQuestProperties.hadleysUpstairs.area,
							BARBARIAN_ASSAULT
						)
					}
				)
			)
		),
		QuestStep(
			name = "Fetch key",
			value = 3,
			items = listOf(
				QuestUtil.requiredItem(Items.CONSUMABLE.LOBSTER, 1, 20),
				QuestUtil.requiredItem(Items.TELEPORT.RING_OF_DUELING, 1),
			),
			operations = listOf(
				Operation(
					name = "Travel to Glarial's tomb",
					entry = { Inventory.contains("Glarial's pebble") || Bank.contains("Glarial's pebble") },
					items = listOf(
						QuestUtil.requiredItem(WaterfallQuestProperties.GLARIALS_PEBBLE, 1),
						QuestUtil.requiredItem(
							Items.TELEPORT.SKILLS_NECKLACE,
							1,
							storage = StorageType.EQUIPPED
						)
					),
					action = {
						val distance = QuestObject.GLARIALS_TOMB.location?.distanceToPlayer()
						if (distance == null || distance > 500) {
							log.debug("Teleporting to Glarial's tomb")
							TeleportHandler.teleportToRegion(GameRegion.UNMAPPED.FISHING_GUILD)
						} else {
							log.debug("Traversing or interacting with Glarial's tombstone")
							if (QuestObject.GLARIALS_TOMB.isVisible() && QuestObject.GLARIALS_TOMB.get()?.isReachable == true) {
								QuestObject.GLARIALS_TOMB.get()?.let {
									val pebble = WaterfallQuestProperties.GLARIALS_PEBBLE.getInventoryItem()
									pebble?.useOn(it)
									Delay.delayTicks(1)
								}
							} else {
								QuestObject.GLARIALS_TOMB.location.let { Navigation.toArea(it) }
							}
							ObjectUtil.interactElseTraverse(
								"Glarial's tombstone",
								"Open",
								coord = WaterfallQuestProperties.prisonDoor,
								teleport = false
							)
						}
					},
				),
				Operation(
					entry = { QuestNPC.GOLRIE.isReachable() },
					action = { QuestNPC.GOLRIE.talkTo() },
				),
				Operation(
					entry = { Inventory.contains("Key") },
					action = {
						ObjectUtil.interactElseTraverse(
							"Door",
							"Open",
							coord = WaterfallQuestProperties.prisonDoor,
							distance = 5
						)
					},
				),

				Operation(
					entry = { !QuestUtil.playerYLessThan(9000) },
					action = {
						ObjectUtil.interactElseTraverse(
							"Crate",
							"Search",
							coord = WaterfallQuestProperties.crate,
							distance = 1
						)
					},
				),
				Operation(
					entry = {
						!WaterfallQuestProperties.castleWarsGnomeStrongholdArdougne.containsPlayer() && QuestUtil.playerYLessThan(
							8000
						)
					},
					items = listOf(
						QuestUtil.requiredItem(Items.TELEPORT.RING_OF_DUELING, 1),
						QuestUtil.requiredItem(Items.CONSUMABLE.LOBSTER, 1, 20)
					),
					action = {
						log.debug("Teleporting to Castle Wars")
						TeleportHandler.teleportToRegion(GameRegion.CASTLE_WARS)
					},
				),
				Operation(
					entry = { WaterfallQuestProperties.treeGnomeVillageDungeonEntrance.isReachable },
					action = { ObjectUtil.interactWithObjects("Ladder", "Climb-down") }
				),
				Operation(
					entry = { QuestUtil.playerYLessThan(8000) },
					action = {
						Navigation.toArea(WaterfallQuestProperties.treeGnomeVillageDungeonEntrance)
					}
				),
			)
		),
		QuestStep(
			name = "Return to Almera",
			value = 4,
			entry = { WaterfallQuestProperties.GLARIALS_PEBBLE.inInventory() || Bank.contains(WaterfallQuestProperties.GLARIALS_PEBBLE.name) },
			fallback = { repeatStep(3) },
			operations = listOf(
				Operation(
					entry = {
						QuestUtil.inInvOrBank(*WaterfallQuestProperties.GLARIALS_ITEMS.map { it.name }.toTypedArray())
					},
					items = listOf(
						QuestUtil.requiredItem(WaterfallQuestProperties.GLARIALS_PEBBLE, 1),
						QuestUtil.requiredItem(WaterfallQuestProperties.GLARIALS_AMULET, 1),
						QuestUtil.requiredItem(WaterfallQuestProperties.GLARIALS_URN, 1),
						QuestUtil.requiredItem(Items.MAGIC.AIR_RUNE, 7, 10),
						QuestUtil.requiredItem(Items.MAGIC.WATER_RUNE, 7, 10),
						QuestUtil.requiredItem(Items.MAGIC.EARTH_RUNE, 7, 10),
						QuestUtil.requiredItem(Items.UTILITY.ROPE, 1, 2),
						QuestUtil.requiredItem(Items.CONSUMABLE.LOBSTER, 1, 14),
					),
					action = { navigateToCliff() }

				),
				Operation(
					entry = { QuestUtil.inInvOrBank(WaterfallQuestProperties.GLARIALS_AMULET.name) && WaterfallQuestProperties.glarialsTomb.containsPlayer() },
					action = { ObjectUtil.interactWithObject("Glarial's Tomb", "Search") },
				),
				Operation(
					entry = { WaterfallQuestProperties.glarialsTomb.containsPlayer() },
					action = { ObjectUtil.interactWithObject("Chest", "Open", "Search") },
					fallback = { repeatStep(3) }
				)

			)
		),
		QuestStep(
			name = "Navigate Waterfall dungeon",
			value = 5,
			entry = { WaterfallQuestProperties.WATERFALL_DUNGEON_AREA.containsPlayer() },
			fallback = { repeatStep(4) },
			operations = listOf(
				Operation(
					entry = { Inventory.contains(WaterfallQuestProperties.CRATE_KEY.id) },
					action = {
						ObjectUtil.interactOrWalkToObjects(
							WaterfallQuestProperties.GLARIAL_DOOR_THIRD,
							WaterfallQuestProperties.GLARIAL_DOOR_SECOND,
							WaterfallQuestProperties.GLARIAL_DOOR_FIRST,
							WaterfallQuestProperties.CRATE_ROOM_DOOR
						)
					}
				),
				Operation(
					entry = { WaterfallQuestProperties.WATERFALL_DUNGEON_CRATE_ROOM.containsPlayer() },
					action = {
						ObjectUtil.interactWithObjects(
							"Crate",
							"Search",
							exitCondition = { Inventory.contains(WaterfallQuestProperties.CRATE_KEY.id) })
					},
					fallback = {
						ObjectUtil.interactOrWalkToObjects(
							ObjectInteraction("Crate", "Search", Coordinate(2590, 9887, 0)),
							WaterfallQuestProperties.CRATE_ROOM_DOOR
						)
					},
				)
			)
		),
		QuestStep(
			name = "Place runes & finish quest",
			value = 6,
			entry = { WaterfallQuestProperties.WATERFALL_DUNGEON_GLARIALS_TOMB.containsPlayer() },
			fallback = { repeatStep(5) },
			operations = listOf(
				Operation(
					entry = { pillarHandler.isComplete() },
					items = listOf(
						QuestUtil.requiredItem(WaterfallQuestProperties.GLARIALS_AMULET, 1),
					),
					action = {
						if (WaterfallQuestProperties.GLARIALS_AMULET.getInventoryItem()
								?.useOn("Statue of Glarial") == true
						) {
							Delay.until({ getProgressValue() != 6 }, 2400)
						}
					}
				),
				Operation(
					entry = {
						Items.MAGIC.AIR_RUNE.inInventory() && Items.MAGIC.WATER_RUNE.inInventory()
								&& Items.MAGIC.EARTH_RUNE.inInventory()
					},
					action = {
						pillarHandler.interactWithPillars()
					}
				),
			)
		),
		QuestStep(
			name = "Finish quest",
			value = 8,
			items = listOf(
				QuestUtil.requiredItem(WaterfallQuestProperties.GLARIALS_URN, 1),
			),
			entry = { WaterfallQuestProperties.WATERFALL_DUNGEON_GLARIALS_TOMB.containsPlayer() },
			fallback = { repeatStep(5) },
			operations = listOf(
				Operation.continueDialog,
				Operation(
					entry = { WaterfallQuestProperties.GLARIALS_URN.inInventory() },
					action = {
						WaterfallQuestProperties.GLARIALS_URN.getInventoryItem()?.useOn("Chalice")
						Delay.until({ getProgressValue() != 8 }, 3600)
					}
				)
			)
		)
	)

	override fun navigateToStartLocation() {
		Navigation.toLocationInRegion(PROPERTIES.startLocation, PROPERTIES.startRegion)
	}

	override fun atStartLocation(): Boolean {
		return QuestNPC.ALMERA.isVisibleReachable()
	}


	private fun boardRaft() {
		log.debug("Boarding raft")
		ObjectUtil.interactElseTraverse(
			"Log raft",
			"Board",
			coord = Coordinate(2511, 3494, 0),
			distance = 2,
			teleport = false
		)
		Execution.delayWhile({ RuneScape.isCutscenePlaying() }, 1800)
	}

	private fun navigateToCliff(enterDungeon: Boolean = true) {
		if (!BARBARIAN_ASSAULT.isPlayerInRegion()) {
			log.debug("Teleporting to Almera")
			TeleportHandler.teleportToRegion(BARBARIAN_ASSAULT)
		}
		when {
			Players.getLocal()?.position == WaterfallQuestProperties.cliffEntrance -> {
				if (enterDungeon) {
					val door = GameObjects.newQuery().names("Door").results().nearest()
					door?.interact("Open")
				}
				Delay.delayTicks(2)
			}

			WaterfallQuestProperties.cliff.containsPlayer() -> {
				log.debug("Climbing cliff")
				Inventory.getItems("Rope").first()?.useOn(2020) // Dead tree
				Delay.delayTicks(3)

			}

			WaterfallQuestProperties.island.containsPlayer() -> {
				log.debug("Traversing to cliff")
				Inventory.getItems("Rope").first()?.useOn(WaterfallQuestProperties.rock)
				Delay.delayTicks(3)
			}

			else -> {
				log.debug("Traversing to raft")
				boardRaft()
			}
		}
	}

	private fun enterDungeon() {
		if (WaterfallQuestProperties.cliff.containsPlayer()) {
			ObjectUtil.interactWithObjects("Door", "Open")
		}
	}


}
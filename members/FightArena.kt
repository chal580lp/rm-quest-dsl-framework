package com.runemate.rebirther.quest.members

import com.runemate.corev2.small.SceneCoordinate
import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.corev2.utility.MyPlayer
import com.runemate.game.api.hybrid.RuneScape
import com.runemate.game.api.hybrid.local.Quest
import com.runemate.game.api.hybrid.local.Skill
import com.runemate.game.api.hybrid.local.VarpID
import com.runemate.game.api.hybrid.local.hud.interfaces.ChatDialog
import com.runemate.game.api.hybrid.local.hud.interfaces.Equipment
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory
import com.runemate.game.api.hybrid.location.Area
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.osrs.local.hud.interfaces.Magic
import com.runemate.game.api.script.Execution
import com.runemate.rebirther.Bot
import com.runemate.rebirther.baseclasses.RequiredItem
import com.runemate.rebirther.details.Def
import com.runemate.rebirther.details.npc.NPCDatabase
import com.runemate.rebirther.handler.ConsumablesHandler
import com.runemate.rebirther.handler.SafeSpotHandler
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.*
import com.runemate.rebirther.quest.members.FightArenaProperties.BOUNCER
import com.runemate.rebirther.quest.members.FightArenaProperties.HEAD_GUARD
import com.runemate.rebirther.quest.members.FightArenaProperties.KHAZARD_BARMAN
import com.runemate.rebirther.quest.members.FightArenaProperties.KHAZARD_OGRE
import com.runemate.rebirther.quest.members.FightArenaProperties.KHAZARD_SCORPION
import com.runemate.rebirther.quest.members.FightArenaProperties.LADY_SERVIL
import com.runemate.rebirther.quest.members.FightArenaProperties.coins
import com.runemate.rebirther.quest.members.FightArenaProperties.fireRunes
import com.runemate.rebirther.quest.members.FightArenaProperties.lobsters
import com.runemate.rebirther.quest.members.FightArenaProperties.mindRunes
import com.runemate.rebirther.quest.members.FightArenaProperties.staffOfAirEquipped
import com.runemate.rebirther.util.SpellCaster


private object FightArenaProperties {

	val LADY_SERVIL = NPCDatabase.QuestNPCDetail(
		name = "Lady Servil",
		ids = setOf(1203),
		gameRegion = GameRegion.TREE_GNOME_VILLAGE,
		location = Area.Rectangular(Coordinate(2563, 3199, 0), Coordinate(2570, 3194, 0)),
		quests = setOf(Quest.OSRS.FIGHT_ARENA)
	)
	val HEAD_GUARD = NPCDatabase.QuestNPCDetail(
		name = "Head Guard",
		ids = setOf(1209),
		gameRegion = GameRegion.TREE_GNOME_VILLAGE,
		location = Coordinate(2614, 3143, 0).area,
		quests = setOf(Quest.OSRS.FIGHT_ARENA)
	)
	val KHAZARD_BARMAN = NPCDatabase.QuestNPCDetail(
		name = "Khazard Barman",
		ids = setOf(1214),
		gameRegion = GameRegion.TREE_GNOME_VILLAGE,
		location = Coordinate(2566, 3140, 0).area,
		quests = setOf(Quest.OSRS.FIGHT_ARENA)
	)

	val KHAZARD_OGRE = NPCDatabase.QuestMonsterDetail(
		name = "Khazard Ogre",
		ids = setOf(1225),
		gameRegion = GameRegion.UNMAPPED,
		combatLevel = 63,
		canSafeSpot = true
	)
	val HENGRAD = NPCDatabase.QuestNPCDetail(
		name = "Hengrad",
		ids = setOf(1218),
		gameRegion = GameRegion.TREE_GNOME_VILLAGE,
		location = Area.Rectangular(Coordinate(2599, 3143, 0), Coordinate(2599, 3143, 0)),
		quests = setOf()
	)
	val KHAZARD_SCORPION = NPCDatabase.QuestMonsterDetail(
		name = "Khazard Scorpion",
		ids = setOf(1226),
		gameRegion = GameRegion.UNMAPPED,
		combatLevel = 44,
		canSafeSpot = true
	)
	val BOUNCER = NPCDatabase.QuestMonsterDetail(
		name = "Bouncer",
		ids = setOf(1224),
		gameRegion = GameRegion.UNMAPPED,
		combatLevel = 137,
		canSafeSpot = true
	)

	val PROPERTIES = quest {
		name = "Fight Arena"
		description = ""
		startRegion = GameRegion.TREE_GNOME_VILLAGE
		startLocation = LADY_SERVIL.location
		questValue = VarpID.QUEST_FIGHT_ARENA.id
		completionValue = 20

		reward(
			questPoints = 2,
			experience = mapOf(
				Skill.ATTACK to 12_175,
				Skill.THIEVING to 2_175
			)
		)
		items {
			lobsters
			mindRunes
			fireRunes
			staffOfAirEquipped
			coins
			teleport(Def.Items.TELEPORT.RING_OF_DUELING)
		}
	}
	val mindRunes = RequiredItem(
		itemDetail = Def.Items.MAGIC.MIND_RUNE,
		quantity = 100,
		withdraw = 1000,
		storage = RequiredItem.StorageType.INVENTORY
	)
	val fireRunes = RequiredItem(
		itemDetail = Def.Items.MAGIC.FIRE_RUNE,
		quantity = 100,
		withdraw = 2000,
		storage = RequiredItem.StorageType.INVENTORY
	)
	val lobsters = RequiredItem(
		itemDetail = Def.Items.CONSUMABLE.LOBSTER,
		quantity = 1,
		withdraw = 15,
		storage = RequiredItem.StorageType.INVENTORY
	)
	val staffOfAirEquipped = RequiredItem(
		itemDetail = Def.Items.EQUIPMENT.STAFF_OF_AIR,
		quantity = 1,
		storage = RequiredItem.StorageType.EQUIPPED
	)
	val coins = RequiredItem(
		itemDetail = Def.Items.GENERAL.COINS,
		quantity = 5,
		withdraw = 50,
		storage = RequiredItem.StorageType.INVENTORY
	)
}


class FightArena(bot: Bot) : QuestDetail(bot, FightArenaProperties.PROPERTIES) {
	override val log = getLogger("FightArena")
	private val safeSpotHandler: SafeSpotHandler by lazy { SafeSpotHandler() }
	private lateinit var consumablesHandler: ConsumablesHandler
	private val armourChest = Coordinate(2613, 3190, 0)
	private val sammysCellEntrance = Coordinate(2617, 3167, 0)

	private val westOfCorpse = SceneCoordinate(46 to 58)
	private val eastOfCorpse = SceneCoordinate(48 to 58)
	private val southOfCorpse = SceneCoordinate(47 to 56)
	private val northOfCorpse = SceneCoordinate(47 to 59)

	private val doorSafeSpot = SceneCoordinate(53 to 48)


	private val useSafeSpot = true

	override fun onStart() {
		super.onStart()
		consumablesHandler = ConsumablesHandler(
			minEatAt = 9,
			maxEatAtPercentage = 50,
			minHealthPercentage = 50,
		)
		bot.eventDispatcher.addListener(consumablesHandler)
	}

	override fun onFinish() {
		super.onFinish()
		bot.eventDispatcher.removeListener(consumablesHandler)
	}

	override fun atStartLocation(): Boolean {
		return LADY_SERVIL.isVisibleReachable()
	}

	override val steps = questSteps {
		step(0) {
			items { listOf(mindRunes, fireRunes, lobsters, staffOfAirEquipped) }
			conversation {
				name { "Talk to Lady Servil" }
				entry { LADY_SERVIL.isVisibleReachable() }
				with { LADY_SERVIL.name }
				options {
					+"Can I help you?"
					+"Yes."
				}
			}
			navigate {
				name { "Navigate to Lady Servil" }
				location { LADY_SERVIL.location }
			}
		}
		step(1) {
			items { listOf(mindRunes, fireRunes, lobsters, staffOfAirEquipped) }
			interact {
				name { "Search Armour Chest" }
				entry { armourChest.isReachable }
				action { "Search" }
				with { sceneObj("Chest", on = Coordinate(2613, 3189, 0)) }
			}
			navigate {
				name { "Navigate to Armour Chest" }
				location { armourChest }
			}
		}
		step(2) {
			items { listOf(mindRunes, fireRunes, lobsters, staffOfAirEquipped) }
			entry {
				(Inventory.contains("Khazard armour") || Equipment.contains("Khazard armour"))
						&& (Inventory.contains("Khazard helmet") || Equipment.contains("Khazard helmet"))
			}
			fallback { repeatStep(1) }
			interact {
				name { "Equip Khazard armour" }
				entry { Inventory.contains("Khazard armour") }
				action { "Wear" }
				with { invItem("Khazard armour") }
			}
			interact {
				name { "Equip Khazard helmet" }
				entry { Inventory.contains("Khazard helmet") }
				action { "Wear" }
				with { invItem("Khazard helmet") }
			}
			navigate {
				name { "Navigate to Guard Door" }
				location { Coordinate(2617, 3172, 0) }
				entry { MyPlayer.y?.let { it > 3175 } ?: false }
			}
			interact {
				name { "Enter Guard door" }
				entry {
					MyPlayer.y?.let { it > 3171 } ?: false
							&& Equipment.contains("Khazard armour")
							&& Equipment.contains("Khazard helmet")
				}
				action { "Open" }
				with { sceneObj("Door", on = Coordinate(2617, 3171, 0)) }
			}
			navigate {
				name { "Navigate to Second Guard Door" }
				location { Coordinate(2616, 3147, 0) }
				entry { MyPlayer.y?.let { it > 3155 } ?: false }
			}
			interact {
				name { "Second Guard door" }
				entry { Equipment.contains("Khazard armour") && Equipment.contains("Khazard helmet") }
				action { "Open" }
				with { sceneObj("Door", on = Coordinate(2616, 3147, 0)) }
				conditions {
					+QueryCondition.REACHABLE
					+QueryCondition.ACTIONS
				}
			}
			conversation {
				name { "Talk to Head Guard" }
				entry { HEAD_GUARD.isVisibleReachable() }
				with { HEAD_GUARD.name }
			}
		}
		step(3) {
			items { listOf(mindRunes, fireRunes, lobsters, staffOfAirEquipped, coins) }
			conversation {
				name { "Talk to Head Guard" }
				entry { HEAD_GUARD.isVisibleReachable() && Inventory.contains("Khali brew") }
				with { HEAD_GUARD.name }
			}
			navigate {
				entry { Inventory.contains("Khali brew") }
				location { HEAD_GUARD.location }
			}
			conversation {
				name { "Talk to Khazard Barman" }
				entry { KHAZARD_BARMAN.isVisibleReachable() && !Inventory.contains("Khali brew") }
				with { KHAZARD_BARMAN.name }
				options {
					+"I'd like a Khali Brew please."
				}
			}
			navigate {
				name { "Navigate to Khazard Barman" }
				entry { !Inventory.contains("Khali brew") }
				location { KHAZARD_BARMAN.location }
			}
		}
		step(5) {
			items { listOf(mindRunes, fireRunes, lobsters, staffOfAirEquipped) }
			entry { Inventory.contains("Khazard cell keys") }
			fallback { repeatStep(3) }
			operation {
				entry { ChatDialog.isOpen() }
				action { ChatDialog.getContinue()?.select() }
				exit { Execution.delay(600) }
			}
			operation {
				entry { RuneScape.isCutscenePlaying() }
				action { log.debug("Waiting for cutscene to finish") }
				exit { Delay.whilst(Delay.CUTSCENE_IS_PLAYING) }
			}
			navigate {
				name { "Navigate to Sammys cell" }
				entry { !MyPlayer.isWithinRange(sammysCellEntrance, 5) }
				location { Coordinate(2617, 3167, 0) }
			}
			interact {
				name { "Prison Gate" }
				entry { Inventory.isItemSelected() }
				action { "Use" }
				with { sceneObj("Prison Gate", on = Coordinate(2617, 3167, 0)) }
				conditions {
					+QueryCondition.REACHABLE
				}
			}
			interact {
				name { "Use keys" }
				entry { Inventory.contains("Khazard cell keys") }
				action { "Use" }
				with { invItem("Khazard cell keys") }
			}
		}
		step(6) {
			items { listOf(mindRunes, fireRunes, lobsters, staffOfAirEquipped) }
			operation {
				name { "Set Autocast spell" }
				entry { useSafeSpot && !Magic.WIND_STRIKE.isAutocasting && !Magic.FIRE_STRIKE.isAutocasting }
				action {
					if (Skill.MAGIC.baseLevel < 13) {
						SpellCaster.setAutocast(SpellCaster.Spell.WIND_STRIKE)
					} else {
						SpellCaster.setAutocast(SpellCaster.Spell.FIRE_STRIKE)
					}
				}
			}
			conversation {
				name { "Re-enter Fight Arena" }
				entry { ChatDialog.isOpen() }
				options {
					+"Yes."
				}
			}
			operation {
				name { "Safespot the Ogre" }
				entry { useSafeSpot && KHAZARD_OGRE.isValid() }
				action {
					SafeSpotHandler.attackFrom(KHAZARD_OGRE.get(), doorSafeSpot.getCoordinate())
				}
			}
			interact {
				name { "Door" }
				action { "Open" }
				entry {
					val doorCoord = Coordinate(2606, 3152, 0)
					log.debug("Door is reachable: ${doorCoord.isReachable}")
					doorCoord.isReachable
				}
				with { sceneObj("Door", on = Coordinate(2606, 3152, 0), addEntryCondition = false) }
				fallback { repeatStep(2) }
			}
		}
		step(8) {
			items { listOf(mindRunes, fireRunes, lobsters, staffOfAirEquipped) }
			conversation {
				name { "Talk to General Khazard & Hengrad" }
				entry { ChatDialog.isOpen() || RuneScape.isCutscenePlaying() }
			}
		}
		step(9) {
			items { listOf(mindRunes, fireRunes, lobsters, staffOfAirEquipped) }
			entry { MyPlayer.isXGreaterThan(5000) || RuneScape.isCutscenePlaying() }
			fallback { repeatStep(6) }
			operation {
				name { "Safespot the Scorpion" }
				entry { useSafeSpot && KHAZARD_SCORPION.isValid() }
				action {
					SafeSpotHandler.attackFrom(
						KHAZARD_SCORPION.get(),
						doorSafeSpot.getCoordinate()
					)
				}
			}
		}
		step(10) {
			items { listOf(mindRunes, fireRunes, lobsters, staffOfAirEquipped) }
			entry { MyPlayer.isXGreaterThan(5000) || RuneScape.isCutscenePlaying() }
			fallback { repeatStep(6) }
			conversation {
				name { "Talk to General Khazard & Hengrad" }
				entry { ChatDialog.isOpen() || RuneScape.isCutscenePlaying() }
			}
			operation {
				name { "Safespot Bouncer" }
				entry { useSafeSpot && BOUNCER.isValid() }
				action {
					safeSpotHandler.attackFrom(
						BOUNCER.get(),
						northOfCorpse.getCoordinate(),
						southOfCorpse.getCoordinate()
					)
				}
			}
		}
//		step(11..14) {
//			//End quest
//		}
	}
}
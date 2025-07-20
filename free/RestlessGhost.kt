package com.runemate.rebirther.quest.free

import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.entities.Npc
import com.runemate.game.api.hybrid.local.Camera
import com.runemate.game.api.hybrid.local.Skill
import com.runemate.game.api.hybrid.local.VarpID
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory
import com.runemate.game.api.hybrid.location.Area
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.hybrid.region.GameObjects
import com.runemate.game.api.hybrid.region.Npcs
import com.runemate.game.api.hybrid.region.Players
import com.runemate.rebirther.Bot
import com.runemate.rebirther.details.Def.Items
import com.runemate.rebirther.details.npc.type.QuestNPC
import com.runemate.rebirther.handler.NpcInteractionHandler.Companion.talkToNPC
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.other.RegionAreas
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.*
import com.runemate.rebirther.quest.framework.Operation
import com.runemate.rebirther.quest.framework.QuestStep
import com.runemate.rebirther.util.QuestUtil.requiredItem
import com.runemate.rebirther.util.QuestUtil.requiredItems


private object RestlessGhostProperties {
	val properties = quest {
		info(
			name = "The Restless Ghost",
			description = "Might get stuck at the very end when handing in the skull.",
			startRegion = GameRegion.LUMBRIDGE,
			startLocation = QuestNPC.FATHER_AERECK.location,
			questValue = VarpID.QUEST_THE_RESTLESS_GHOST.id,
			completionValue = 5,
		)
		reward(
			questPoints = 1,
			experience = mapOf(Skill.PRAYER to 1125)
		)
		npcs {
			friendly(QuestNPC.FATHER_AERECK)
			friendly(QuestNPC.FATHER_URHNEY)
		}
		items {
			teleport(Items.TELEPORT.LUMBRIDGE_TELEPORT_TAB, withdraw = 5)
			teleport(Items.TELEPORT.NECKLACE_OF_PASSAGE)
			teleport(Items.TELEPORT.RING_OF_WEALTH)
		}
	}
}

class RestlessGhost(bot: Bot) : QuestDetail(bot, RestlessGhostProperties.properties) {
	override val log = getLogger("RestlessGhost")

	private val fatherAereck: Npc?
		get() = QuestNPC.FATHER_AERECK.get()

	private val fatherUrhney: Npc?
		get() = QuestNPC.FATHER_URHNEY.get()
	private val coffinHouse = Area.Rectangular(Coordinate(3247, 3195, 0), Coordinate(3252, 3190, 0))

	override val steps = listOf(
		QuestStep(
			name = "Start the quest",
			value = 0,
			entry = { fatherAereck?.isVisible == true },
			operation = Operation(
				name = "Talk to Father Aereck",
				action = { startQuest() },
				fallback = { navigateToStartLocation() }
			)
		),
		QuestStep(
			name = "Find Father Urhney",
			value = 1,
			entry = { GameRegion.LUMBRIDGE.isPlayerInRegion() || GameRegion.LUMBRIDGE_SWAMP.isPlayerInRegion() },
			operation = Operation(
				name = "Find Father Urhney",
				action = { findFatherUrhney() },
				fallback = { Navigation.toRegion(PROPERTIES.startRegion) }
			)
		),
		QuestStep(
			name = "Locate marble coffin",
			value = 2,
			items = requiredItems(
				requiredItem(Items.QUEST.GHOSTSPEAK_AMULET, 1)
			),
			entry = { GameRegion.LUMBRIDGE.isPlayerInRegion() || GameRegion.LUMBRIDGE_SWAMP.isPlayerInRegion() },
			operation = Operation(
				name = "Locate marble coffin",
				action = { locateMarbleCoffin() },
				fallback = { Navigation.toRegion(PROPERTIES.startRegion) }
			)
		),
		QuestStep(
			name = "Fetch skull",
			value = 3,
			entry = { GameRegion.WIZARDS_TOWER.isPlayerInRegion() },
			fallback = { Navigation.toRegion(GameRegion.WIZARDS_TOWER) },
			operation = Operation(
				name = "Fetch skull",
				action = { fetchSkull() },
			)
		),
		QuestStep(
			name = "Hand in skull",
			value = 4,
			entry = { Inventory.contains("Ghost's skull") },
			fallback = { fetchSkull() },
			operation = Operation(
				name = "Hand in skull",
				action = { handInSkull() },
			)
		)
	)

	private fun startQuest() {
		log.info("Starting ${PROPERTIES.name}")
		talkToNPC(fatherAereck, listOf("I'm looking for a quest!", "Yes."))
	}

	private fun findFatherUrhney() {
		log.info("Finding Father Urhney")
		if (fatherUrhney?.isVisible == true && fatherUrhney?.area?.isReachable == true) {
			talkToNPC(
				fatherUrhney,
				listOf("Father Aereck sent me to talk to you.", "He's got a ghost haunting his graveyard.")
			)
			return
		}
		Navigation.toArea(QuestNPC.FATHER_URHNEY.location)

	}

	private fun locateMarbleCoffin() {
		if (Inventory.contains(Items.QUEST.GHOSTSPEAK_AMULET.id)) {
			log.info("Equipping the Ghostspeak amulet")
			Inventory.newQuery().ids(Items.QUEST.GHOSTSPEAK_AMULET.id).results().first()?.interact("Wear")
		}
		if (!traverseToGhost()) {
			return
		}
		val ghost = Npcs.newQuery().ids(922).results().firstOrNull()
		if (ghost != null) {
			talkToNPC(ghost, listOf("Yep, now tell me what the problem is."))
			return
		}

	}

	private fun traverseToGhost(): Boolean {
		if (!GameRegion.LUMBRIDGE.isPlayerInRegion() && !GameRegion.LUMBRIDGE_SWAMP.isPlayerInRegion()) {
			log.debug("Player is not in Lumbridge or Lumbridge Swamp. Traversing to Lumbridge")
			Navigation.toRegion(GameRegion.LUMBRIDGE)
			return false
		}
		if (!coffinHouse.contains(Players.getLocal())) {
			log.debug("CoffinHouse does not contain player. Traversing to coffin house.")
			Navigation.toArea(Coordinate(3248, 3193, 0).area)
			return false
		}
		val ghost = Npcs.newQuery().ids(922).results().firstOrNull()
		if (ghost == null) {
			log.debug("Ghost not found. Searching for coffin")
			val coffin = GameObjects.newQuery().names("Coffin").results().first()
			if (coffin?.isVisible == true) {
				log.debug("Coffin is visible, interacting")
				if (coffin.interact("Open") == false) coffin.interact("Search")
				Delay.untilChatOptionsAvailable()
			}
			return false
		}
		return true
	}

	private fun fetchSkull() {
		if (RegionAreas.WIZARDS_TOWER_BASEMENT_AREA.contains(Players.getLocal())) {
			if (GameRegion.WIZARDS_TOWER.altar.contains(Players.getLocal())) {
				log.info("Fetching the skull")
				val altar = GameObjects.newQuery().names("Altar").results().first()
				if (altar?.isVisible == true || Camera.turnTo(altar)) {
					altar?.interact("Search")
					Delay.untilChatOptionsAvailable()
				}
			} else {
				Navigation.toArea(GameRegion.WIZARDS_TOWER.altar)
			}
		} else {
			traverseToWizardTowerStairs()
		}
	}

	private fun traverseToWizardTowerStairs() {
		if (!GameRegion.WIZARDS_TOWER.isPlayerInRegion()) {
			Navigation.toRegion(GameRegion.WIZARDS_TOWER)
			return
		}
		Navigation.toWizardTowerBasement()
	}

	private fun handInSkull() {
		if (!traverseToGhost()) {
			return
		}
		Inventory.newQuery().names("Ghost's skull").results().firstOrNull().let {
			it?.interact("Use", "Coffin")
		}
		val ghost = Npcs.newQuery().ids(922).results().firstOrNull()
		if (ghost != null) {
			val coffin = GameObjects.newQuery().names("Coffin").results().firstOrNull()
			coffin?.interact("Search")
			Delay.delayTicks(5)
		}
	}

	override fun navigateToStartLocation() {
		log.debug("Traversing to the start location")
		if (!PROPERTIES.startRegion.isPlayerInRegion()) {
			Navigation.toRegion(PROPERTIES.startRegion)
		} else {
			Navigation.toArea(PROPERTIES.startLocation)
		}
	}

	override fun atStartLocation(): Boolean {
		return fatherAereck?.isVisible == true && fatherAereck?.area?.isReachable == true
	}
}
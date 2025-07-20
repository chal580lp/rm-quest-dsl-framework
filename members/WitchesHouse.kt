package com.runemate.rebirther.quest.members

import com.runemate.corev2.extensions.containsPlayer
import com.runemate.corev2.extensions.coord
import com.runemate.corev2.extensions.distanceToPlayer
import com.runemate.corev2.extensions.open
import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.corev2.utility.util
import com.runemate.corev2.utility.util.getNearestDoor
import com.runemate.game.api.hybrid.entities.Npc
import com.runemate.game.api.hybrid.local.Camera
import com.runemate.game.api.hybrid.local.Skill
import com.runemate.game.api.hybrid.local.VarpID
import com.runemate.game.api.hybrid.local.hud.interfaces.ChatDialog
import com.runemate.game.api.hybrid.local.hud.interfaces.Equipment
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory
import com.runemate.game.api.hybrid.location.Area
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.hybrid.region.GameObjects
import com.runemate.game.api.hybrid.region.GroundItems
import com.runemate.game.api.hybrid.region.Npcs
import com.runemate.game.api.hybrid.region.Players
import com.runemate.game.api.osrs.local.hud.interfaces.Magic
import com.runemate.rebirther.Bot
import com.runemate.rebirther.baseclasses.RequiredItem.StorageType.EQUIPPED
import com.runemate.rebirther.details.Def.Items
import com.runemate.rebirther.details.npc.type.QuestMonsterNPC.WitchExperimentFour
import com.runemate.rebirther.details.npc.type.QuestMonsterNPC.WitchExperimentOne
import com.runemate.rebirther.details.npc.type.QuestMonsterNPC.WitchExperimentThree
import com.runemate.rebirther.details.npc.type.QuestMonsterNPC.WitchExperimentTwo
import com.runemate.rebirther.details.npc.type.QuestNPC
import com.runemate.rebirther.handler.ConsumablesHandler
import com.runemate.rebirther.handler.NpcInteractionHandler.Companion.talkToNPC
import com.runemate.rebirther.handler.SafeSpotHandler
import com.runemate.rebirther.handler.SafeSpotHandler.Companion.attackFrom
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.other.GameRegion.TAVERLEY
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.*
import com.runemate.rebirther.quest.framework.Operation
import com.runemate.rebirther.quest.framework.QuestStep
import com.runemate.rebirther.quest.members.WitchesHouseProperties.properties
import com.runemate.rebirther.util.CoordinateTraversal
import com.runemate.rebirther.util.QuestUtil.requiredItem
import com.runemate.rebirther.util.QuestUtil.requiredItems
import com.runemate.rebirther.util.SpellCaster
import kotlin.math.abs

private object WitchesHouseProperties {
	val properties = quest {
		info(
			name = "Witch's House",
			description = "Requires killing levels 19-53 monsters, will safe spot with wind strike.",
			startRegion = TAVERLEY,
			startLocation = QuestNPC.BOY.location,
			questValue = VarpID.QUEST_WITCHS_HOUSE.id,
			completionValue = 7,
			beta = true
		)
		reward(
			questPoints = 4,
			experience = mapOf(Skill.CONSTITUTION to 6325),
		)

		npcs {
			friendly(QuestNPC.BOY)
			monster(WitchExperimentOne)
			monster(WitchExperimentTwo)
			monster(WitchExperimentThree)
			monster(WitchExperimentFour)
		}

		items {
			inventory(Items.INGREDIENT.CHEESE, quantity = 1, withdraw = 4)
			inventory(Items.CONSUMABLE.LOBSTER, quantity = 10)
			inventory(Items.MAGIC.MIND_RUNE, quantity = 300, withdraw = 1000)
			equipment(Items.EQUIPMENT.STAFF_OF_AIR)
			teleport(Items.TELEPORT.FALADOR_TELEPORT_TAB, withdraw = 5)
			teleport(Items.TELEPORT.RING_OF_WEALTH)
		}
	}
}

class WitchesHouse(bot: Bot) : QuestDetail(bot, properties) {
	override val log = getLogger("WitchesHouse")
	private lateinit var consumablesHandler: ConsumablesHandler
	private val safeSpotHandler: SafeSpotHandler = SafeSpotHandler()

	private val boy: Npc?
		get() = QuestNPC.BOY.get()

	private val firstSafeSpot = coord(2936, 3465)
	private val firstNpcSpot = coord(2937, 3466)
	private val firstNpcGoSpot = coord(2937, 3465)

	private val witchesHouseEntrance = coord(2900, 3473)
	private val witchesHouse = Area.Rectangular(coord(2901, 3466), coord(2907, 3476))
	private val basement = Area.Rectangular(coord(2896, 9878), coord(2909, 9869))
	private val mouseRoom = Area.Rectangular(coord(2901, 3467), coord(2903, 3466))
	private val shed = Area.rectangular(coord(2934, 3467), coord(2937, 3459))

	private val courtyard = Area.Polygonal(
		Coordinate(2900, 3465, 0),
		Coordinate(2900, 3459, 0),
		Coordinate(2934, 3459, 0),
		Coordinate(2934, 3468, 0),
		Coordinate(2913, 3468, 0),
		Coordinate(2913, 3477, 0),
		Coordinate(2908, 3477, 0),
		Coordinate(2908, 3467, 0),
		Coordinate(2904, 3465, 0),
		Coordinate(2900, 3466, 0),
		Coordinate(2900, 3465, 0)
	)

	private val courtyardCoordsRight = listOf(
		coord(2903, 3460), coord(2909, 3460), coord(2917, 3460),
		coord(2925, 3460), coord(2933, 3460)
	)

	private val courtyardCoordsLeft = listOf(
		coord(2932, 3466), coord(2927, 3466), coord(2920, 3466),
		coord(2913, 3466)
	)

	override fun onStart() {
		log.info("onStart ${PROPERTIES.name}")
		consumablesHandler = ConsumablesHandler(
			minEatAt = 6,
			maxEatAtPercentage = 50,
			minHealthPercentage = 50,
		)
		bot.eventDispatcher.addListener(consumablesHandler)
	}

	override fun onFinish() {
		log.info("onFinish ${PROPERTIES.name}")
		bot.eventDispatcher.removeListener(consumablesHandler)
	}


	override val steps = listOf(
		QuestStep(
			name = "Start the quest",
			value = 0,
			entry = { boy?.isVisible == true },
			operation = Operation(
				name = "Talk to boy",
				action = { startQuest() },
			),
			fallback = { navigateToStartLocation() }
		),
		QuestStep(
			name = "Navigate to Taverley",
			value = 1,
			items = requiredItems(
				requiredItem(Items.INGREDIENT.CHEESE, 1, 4),
				requiredItem(Items.EQUIPMENT.STAFF_OF_AIR, 1, storage = EQUIPPED),
				requiredItem(Items.MAGIC.MIND_RUNE, 300, 1000),
				requiredItem(Items.CONSUMABLE.LOBSTER, 1, withdraw = 10)
			),
			entry = { TAVERLEY.isPlayerInRegion() || basement.containsPlayer() },
			operation = Operation(
				name = "Navigate house",
				action = { navigateHouse() },
			),
			fallback = { Navigation.toRegion(TAVERLEY) }

		),
		QuestStep(
			name = "Continue navigating house",
			value = 2,
			entry = { TAVERLEY.isPlayerInRegion() || basement.containsPlayer() },
			operation = Operation(
				name = "Navigate house",
				action = { navigateHouse() },
			),
			fallback = { Navigation.toRegion(TAVERLEY) }
		),
		QuestStep(
			name = "Navigate to courtyard",
			value = 3,
			items = requiredItems(
				requiredItem(Items.EQUIPMENT.STAFF_OF_AIR, 1, storage = EQUIPPED),
				requiredItem(Items.MAGIC.MIND_RUNE, 300, 1000),
				requiredItem(Items.CONSUMABLE.LOBSTER, 1, withdraw = 10)
			),
			entry = { TAVERLEY.isPlayerInRegion() },
			operation = Operation(
				name = "Navigate courtyard",
				action = { navigateCourtyard() },
			),
			fallback = { Navigation.toRegion(TAVERLEY) }
		),
		QuestStep(
			name = "Finish quest",
			value = 6,
			entry = { TAVERLEY.isPlayerInRegion() || basement.containsPlayer() },
			operation = Operation(
				name = "Finish quest",
				action = { finishQuest() },

				),
			fallback = { Navigation.toRegion(TAVERLEY) }
		)
	)

	private fun startQuest() {
		log.info("Starting ${PROPERTIES.name}")
		talkToNPC(boy, listOf("What's the matter?", "Yes."))
	}

	private fun navigateHouse() {
		if (!Inventory.contains("Door key")) {
			fetchDoorKey()
			return
		}
		if (basement.contains(Players.getLocal())) {
			handleBasement()
			return
		}
		if (witchesHouse.contains(Players.getLocal())) {
			if (Inventory.contains("Magnet")) {
				handleMouse()
				return
			} else {
				climbDownLadder()
				return
			}
		}
		if (!witchesHouse.contains(Players.getLocal())) {
			enterHouse()
			return
		}

	}

	private fun enterHouse() {
		log.debug("Traversing inside the house")

		if (witchesHouseEntrance.distanceToPlayer()?.let { it <= 5 } == true) {
			val door = getNearestDoor()
			door?.open()
			Delay.until({ witchesHouse.contains(Players.getLocal()) })
		} else {
			log.debug("Traversing to the house")
			Navigation.toArea(witchesHouse)
		}
		return
	}

	private fun climbDownLadder() {
		val ladder =
			GameObjects.newQuery().names("Ladder").actions("Climb-down").surroundingsReachable().results().nearest()
		if (ladder != null) {
			ladder.interact("Climb-down")
			Delay.until({ !witchesHouse.contains(Players.getLocal()) })
		} else {
			log.debug("Opening door in the way")
			val insideDoor = util.getObjectOn(coord(2902, 3474))
			insideDoor.let {
				it?.open()
			}
		}
	}

	private fun fetchDoorKey() {
		log.debug("Fetching the door key")
		val pottedPlant = GameObjects.newQuery().actions("Look-under").results().nearest()
		if (pottedPlant != null && pottedPlant.distanceTo(Players.getLocal()) <= 7) {
			if (pottedPlant.isVisible) Camera.turnTo(pottedPlant)
			pottedPlant.interact("Look-under")
			Delay.until({ Inventory.contains("Door key") })
		} else {
			if (TAVERLEY.isPlayerInRegion()) {
				Navigation.toCoord(coord(2899, 3472))
			} else {
				Navigation.toRegion(TAVERLEY)
			}
		}

	}

	private fun handleBasement() {
		log.debug("Handling the basement")
		if (!Equipment.contains("Leather gloves")) {
			fetchGloves()
			return
		}
		if (!Inventory.contains("Magnet")) {
			fetchMagnet()
			return
		}
		val ladder =
			GameObjects.newQuery().names("Ladder").actions("Climb-up").surroundingsReachable().results().nearest()
		if (ladder != null) {
			ladder.interact("Climb-up")
			Delay.until({ !basement.contains(Players.getLocal()) })
		} else {
			log.debug("Opening gate in the way")
			val gate = GameObjects.newQuery().names("Gate").actions("Open").results().nearest()
			gate.let {
				it?.open()
			}
		}
	}

	private fun fetchMagnet() {
		val cupboard = GameObjects.newQuery().names("Cupboard").surroundingsReachable().results().nearest()
		if (cupboard != null) {
			cupboard.let {
				it.interact("Open")
				Delay.whilst(Delay.MOVING_OR_ACTIVE, 2400)
				it.interact("Search")
			}
			Delay.untilInventoryContains("Magnet")
		} else {
			val gate = GameObjects.newQuery().names("Gate").actions("Open").results().nearest()
			gate.let {
				it?.open()
			}
		}
	}

	private fun fetchGloves() {
		if (!Items.EQUIPMENT.LEATHER_GLOVES.inInventoryOrEquipment()) {
			val box = GameObjects.newQuery().names("Boxes").actions("Search").surroundingsReachable().results().first()
			repeat(10) {
				if (Inventory.isFull() || Inventory.contains("Leather gloves")) {
					return@repeat
				}
				box.let {
					it?.interact("Search")
					Delay.delayTicks(1)
				}
			}
			if (Inventory.isFull()) {
				Inventory.getItems("Cabbage", "Needle", "Leather boots", "Thread").forEach {
					it.interact("Drop")
				}
			}
			return
		}
		if (!Equipment.contains("Leather gloves")) {
			val gloves = Inventory.newQuery().names("Leather gloves").results().first()
			gloves?.interact("Wear")
		}
	}

	private fun handleMouse() {
		log.debug("Handling the mouse")
		if (mouseRoom.contains(Players.getLocal())) {
			log.debug("Searching for the mouse")
			val cheese = GroundItems.newQuery().names("Cheese").results()
			if (cheese.isEmpty()) {
				Inventory.getItems("Cheese")?.firstOrNull()?.interact("Drop")
				Delay.delayTicks(1)
			}
			val mouse = Npcs.newQuery().names("Mouse").results().nearest()
			mouse.let {
				Inventory.getItems("Magnet")?.firstOrNull()?.interact("Use")
				Delay.delayTicks(1)
				if (mouse?.interact("Use") == true) {
					Delay.delayTicks(1)
				}
			}
		} else {
			if (!Navigation.viaScene(mouseRoom)) {
				log.debug("Opening door in the way")
				val door =
					GameObjects.newQuery().names("Door").actions("Open").filter { it.position?.x == 2902 }.results()
						.nearest()
				door.let {
					it?.open()
				}
			}
		}
	}

	private fun navigateCourtyard() {
		if (!courtyard.contains(Players.getLocal()) && !shed.contains(Players.getLocal())) {
			if (getProgressValue() < 3 || !witchesHouse.containsPlayer()) {
				navigateHouse()
				return
			}
			enterCourtyard()

		} else {
			if (Inventory.contains("Key") || getProgressValue() >= 4) {
				if (shed.contains(Players.getLocal())) {
					handleShed()
					return
				} else {
					enterShed()
					return
				}
			}
			val fountain = GameObjects.newQuery().names("Fountain").actions("Check").results().nearest()
			if (fountain != null && fountain.distanceTo(Players.getLocal()) <= 8) {
				fountain.interact("Check")
				Delay.whilst(Delay.MOVING_OR_ACTIVE)
				ChatDialog.getContinue()?.select()
			} else {
				log.debug("Traversing to the fountain")
				CoordinateTraversal.traverseCoordinates(
					courtyardCoordsRight + courtyardCoordsLeft,
					canWalk = this::canWalk
				)
			}
		}
	}

	private fun exitCourtyard() {
		val x = Players.getLocal()?.position?.x ?: return
		if (x > courtyardCoordsRight.first().x) {
			CoordinateTraversal.traverseCoordinates(courtyardCoordsRight.reversed(), canWalk = this::canWalk)
		} else {
			val door = GameObjects.newQuery().names("Door").actions("Open").filter {
				(it?.position?.y ?: 0) >= (Players.getLocal()?.position?.y ?: 0) && (it?.position?.y ?: 3475) < 3474
			}.results().first()
			door.let {
				it?.open()
			}
		}
	}

	private fun enterCourtyard() {
		val coord = coord(2901, 3464)
		if (!Navigation.viaScene(coord.area)) {
			val position = Players.getLocal()?.position ?: return
			val door =
				GameObjects.newQuery().names("Door").actions("Open").surroundingsReachable().filter { gameObject ->
					gameObject.position?.let {
						it.y < position.y
					} ?: false
				}.results().nearest()
			door.let {
				it?.open()
				Delay.until({ courtyard.contains(Players.getLocal()) })
			}

		}
	}

	private fun canWalk(coord: Coordinate): Boolean {
		val player = Players.getLocal() ?: return false.also { log.info("Can't walk: Player not found") }
		if (player.isMoving) return false.also { log.info("Can't walk: Player is moving") }

		val witch = Npcs.newQuery().names("Nora T. Hagg").results().nearest()
			?: return true.also { log.info("Can walk: Witch not found") }
		val witchX = witch.position?.x ?: return false.also { log.info("Can't walk: Witch position unknown") }
		val playerX = player.position?.x ?: return false.also { log.info("Can't walk: Player position unknown") }

		// 90 is west, 270 is east
		val witchFacingWest = witch.orientationAsAngle == 90
		val witchFacingEast = witch.orientationAsAngle == 270

		val witchDirection = when {
			witchFacingWest -> "west"
			witchFacingEast -> "east"
			else -> "other"
		}

		log.info(
			"Witch direction: {} at: {}, Player at: {}, Target: {}",
			witchDirection, witchX, playerX, coord.x
		)

		val movingEast = coord.x > playerX
		val movingWest = coord.x < playerX

		val movementDirection = if (movingEast) "east" else "west"
		val distanceToMove = abs(coord.x - playerX)
		val distanceToWitch = abs(witchX - playerX)

		fun logMovement(canMove: Boolean, reason: String): Boolean {
			log.info(
				"Can {}move {}: {}. Distance to move: {}, Distance to witch: {}",
				if (canMove) "" else "not ",
				movementDirection,
				reason,
				distanceToMove,
				distanceToWitch
			)
			return canMove
		}

		return when {
			movingEast && witchFacingWest && witchX <= playerX ->
				logMovement(true, "Witch is facing west and is behind or at player's position")

			movingEast && witchFacingEast && witchX >= coord.x ->
				logMovement(true, "Witch is facing east but is beyond or at target position")

			movingWest && witchFacingEast && witchX >= playerX ->
				logMovement(true, "Witch is facing east and is ahead of or at player's position")

			movingWest && witchFacingWest && witchX <= coord.x ->
				logMovement(true, "Witch is facing west but is behind or at target position")

			else ->
				logMovement(false, "Movement blocked by witch")
		}
	}

	private fun enterShed() {
		val x = Players.getLocal()?.position?.x ?: return
		log.debug("Player x is $x")
		if (x == 2933 || x == 2932) {
			GameObjects.newQuery().names("Door").actions("Open").results().nearest().let {
				Inventory.newQuery().names("Key").results().firstOrNull()?.interact("Use")
				it?.interact("Use")
				Delay.whilst(Delay.MOVING_OR_ACTIVE)
			}
			Navigation.toArea(shed.center.area)
		} else {
			val y = Players.getLocal()?.position?.y ?: return
			val path = if (y >= 3466) courtyardCoordsLeft.reversed() else courtyardCoordsRight
			log.debug("Traversing to the shed from the {}", if (y >= 3466) "left" else "right")
			CoordinateTraversal.traverseCoordinates(path, canWalk = this::canWalk)
		}
	}

	private fun exitShed() {
		log.debug("Exiting the shed")
		GameObjects.newQuery().names("Door").actions("Open").results().nearest().let {
			it?.open()
		}
	}

	private fun handleShed() {
		log.debug("Handling the shed")
		if (!configureCombat()) return
		val expirementOneAndTwo =
			Npcs.newQuery().names(WitchExperimentOne.name, WitchExperimentTwo.name).results().nearest()
		val expirementThreeAndFour = Npcs.newQuery().names(
			WitchExperimentThree.name,
			WitchExperimentFour.name
		).results().nearest()
		if (expirementOneAndTwo != null) {
			safeSpotHandler.moveNpcAttack(expirementOneAndTwo, firstSafeSpot, firstNpcSpot, firstNpcGoSpot)
			return
		}
		if (expirementThreeAndFour != null) {
			attackFrom(expirementThreeAndFour, coord(2936, 3459))
		} else {
			log.debug("No monsters found in shed.")
		}
	}

	private fun configureCombat(): Boolean {
		if (!Magic.WIND_STRIKE.isAutocasting) {
			SpellCaster.setAutocast(SpellCaster.Spell.WIND_STRIKE)
			return false
		}
		return true
	}

	private fun finishQuest() {
		if (Inventory.contains("Ball")) {
			if (shed.containsPlayer()) {
				log.debug("Exiting the shed")
				exitShed()
				return
			}
			if (courtyard.containsPlayer()) {
				log.debug("Exiting the courtyard")
				exitCourtyard()
				return
			}
			if (witchesHouse.containsPlayer()) {
				log.debug("Exiting the house")
				val door =
					GameObjects.newQuery()
						.names("Door")
						.actions("Open")
						.filter { it?.position?.y ?: 0 > Players.getLocal()?.position?.y ?: 0 || it.position == witchesHouseEntrance }
						.results()
						.nearest()
				door?.open()
				return
			}
			if (QuestNPC.BOY.isVisibleReachable()) {
				talkToNPC(boy)
			} else {
				log.debug("Traversing to the start location")
				navigateToStartLocation()
			}

		} else {
			if (shed.containsPlayer()) {
				collectBall()
			} else {
				navigateCourtyard()
			}
		}
	}

	private fun collectBall() {
		Camera.turnTo(1.0)
		val ball = GroundItems.newQuery().names("Ball").results().nearest()
		ball?.click()
		Delay.untilInventoryContains("Ball")

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
		return boy?.isVisible == true && boy?.area?.isReachable == true
	}
}
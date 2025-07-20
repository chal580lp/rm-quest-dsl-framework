package com.runemate.rebirther.quest.members.recruitmentdrive

import com.runemate.corev2.extensions.distanceToPlayer
import com.runemate.corev2.extensions.name
import com.runemate.corev2.extensions.useOn
import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.corev2.utility.VarbitRequirement
import com.runemate.game.api.hybrid.local.hud.interfaces.ChatDialog
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.hybrid.region.GameObjects
import com.runemate.game.api.hybrid.region.GroundItems
import com.runemate.game.api.hybrid.region.Players
import com.runemate.game.api.hybrid.util.Regex
import com.runemate.game.api.script.Execution
import com.runemate.rebirther.util.ObjectUtil.interactWithObject

class MsCheevesRoom {
	private val log = getLogger("MsCheevesRoom")

	// Varbit requirements
	private val hasRetrievedThreeVials = VarbitRequirement(679, 3)
	private val hasSpadeHeadOnDoor = VarbitRequirement(686, 1)
	private val hasCupricSulfateOnDoor = VarbitRequirement(687, 1)
	private val hasVialOfLiquidOnDoor = VarbitRequirement(686, 2)
	private val hasFirstDoorOpen = VarbitRequirement(686, 3)
	private val hasLiquidInTin = VarbitRequirement(689, 1)
	private val finishedRoom = VarbitRequirement(664, 1)

	fun execute(): Boolean {
//		if (handleChatDialog()) {
//			Delay.delayTicks(1)
//			return true
//		}
		return when {
			handleChatDialog() -> Execution.delay(600)
			finishedRoom.check() -> leaveRoom()
			hasFirstDoorOpen.check() -> handleSecondDoorKey()
			hasVialOfLiquidOnDoor.check() -> openFirstDoor()
			hasCupricSulfateOnDoor.check() -> useVialOfLiquidOnDoor()
			hasSpadeHeadOnDoor.check() -> useCupricSulfateOnDoor()
			Inventory.contains(METAL_SPADE_HANDLE) -> useSpadeHeadOnDoor()
			hasItem("METAL SPADE") -> useSpadeOnBunsenBurner()
			else -> collectRequiredItems()
		}
	}

	private fun collectRequiredItems(): Boolean {
		log.debug("Collecting required items")
		return when {
			!hasItem("Magnet") -> collectItem("Magnet", "Old Bookshelf", on = Coordinate(2468, 4943, 0))
			!hasItem("Cupric sulfate") -> collectItem("Cupric sulfate", "Shelves", on = Coordinate(2472, 4944, 0))
			!hasItem("Gypsum") -> collectItem("Gypsum", "Shelves", on = Coordinate(2473, 4944, 0))
			!hasItem("Sodium chloride") -> collectItem("Sodium chloride", "Shelves", on = Coordinate(2474, 4944, 0))
			!hasItem("Bronze wire") -> collectItem("Bronze wire", "Crate", on = Coordinate(2475, 4943, 0))
			!hasItem("Tin") -> collectItem("Tin", "Crate", on = Coordinate(2476, 4943, 0))
			//!hasItem("Shears") -> collectItem("Shears", "Closed chest", Coordinate(2477, 4942, 0))
			!hasItem("Shears") -> collectItem("Shears", "Closed chest", "Open chest", on = Coordinate(2477, 4942, 0))
			!hasItem("Chisel") -> collectItem("Chisel", "Crate", on = Coordinate(2476, 4937, 0))
			!hasItem("Nitrous oxide") -> collectItem("Nitrous oxide", "Shelves", on = Coordinate(2474, 4936, 0))
			!hasItem("Tin ore powder") -> collectItem("Tin ore powder", "Shelves", on = Coordinate(2473, 4936, 0))
			!hasItem("Cupric ore powder") -> collectItem("Cupric ore powder", "Shelves", on = Coordinate(2472, 4936, 0))
			!hasItem("Vial of liquid") -> collectItem("Vial of liquid", "Shelves", on = Coordinate(2471, 4936, 0))
			!hasItem("Knife") -> collectItem("Knife", "Old Bookshelf", on = Coordinate(2468, 4937, 0))

			!hasItem("Acetic acid") -> collectItem("Acetic acid", "Shelves", on = Coordinate(2471, 4944, 0))

			//!hasRetrievedThreeVials.check() -> collectItem("VIAL OF LIQUID", "Shelves", on = Coordinate(2471, 4944, 0))
			!hasItem("METAL SPADE") -> collectMetalSpade()
			else -> false
		}
	}

	private fun collectItem(itemName: String, vararg objectName: String, on: Coordinate): Boolean {
		val gameObject = GameObjects.newQuery().names(*objectName).on(on).results().firstOrNull()
		val inventorySize = Inventory.getEmptySlots()
		if (interactWithObject(gameObject, "Search", "Open")) {
			log.debug("Interaction successful, collecting $itemName from $objectName")
			Delay.until({ Inventory.getEmptySlots() < inventorySize || ChatDialog.isOpen() }, 2400)
			handleChatDialog()
			Delay.delayTicks(1)
			return true
		}
		return false
	}

	private fun collectMetalSpade(): Boolean {
		val spade = GroundItems.newQuery().names("Metal spade").results().firstOrNull()
		return spade?.interact("Take") == true && Delay.until({ hasItem("METAL SPADE") }, 2400)
	}

	private fun handleChatDialog(): Boolean {
		if (!ChatDialog.isOpen()) return false
		val chatOptions = ChatDialog.getOptions()
		if (chatOptions.isEmpty()) {
			ChatDialog.getContinue()?.select()
			return true
		}
		val both = Regex.getPatternForContainsString("Take both vials.")
		val three = Regex.getPatternForContainsString("Take all three vials")
		val yes = Regex.getPatternForContainsString("YES")
		return when {
			chatOptions.any { both.matcher(it.text).matches() } -> ChatDialog.getOption(both)?.select() == true

			chatOptions.any { three.matcher(it.text).matches() } -> ChatDialog.getOption(three)?.select() == true

			chatOptions.any { yes.matcher(it.text).matches() } -> ChatDialog.getOption(yes)?.select() == true
			else -> {
				false
			}
		}
	}

	private fun useSpadeOnBunsenBurner(): Boolean {
		return useItemOnObject("METAL SPADE", "Bunsen burner")
	}

	private fun useSpadeHeadOnDoor(): Boolean {
		return useItemOnObject("METAL SPADE", "Stone Door")
	}

	private fun useCupricSulfateOnDoor(): Boolean {
		return useItemOnObject("CUPRIC SULFATE", "Stone Door")
	}

	private fun useVialOfLiquidOnDoor(): Boolean {
		return useItemOnObject("VIAL OF LIQUID", "Stone Door")
	}

	private fun openFirstDoor(): Boolean {
		return interactWithObject("Stone Door", "Pull-spade")
	}

	private fun handleSecondDoorKey(): Boolean {
		log.debug("Handling second door key")
		return when {
			Inventory.contains("BRONZE KEY") -> leaveRoom()
			!hasLiquidInTin.check() -> useItemOnItem("VIAL OF LIQUID", "TIN")
			hasLiquidInTin.check() -> useItemOnItem("GYPSUM", "TIN")
			Inventory.contains(TIN_FULL_OF_GYPSUM) -> useItemOnObject("TIN FULL OF GYPSUM", "Key")
			Inventory.contains(TIN_WITH_KEY_PRINT) -> useItemOnItem("CUPRIC ORE POWDER", "TIN FULL WITH KEY PRINT")
			Inventory.contains(TIN_WITH_CUPRIC_ORE) -> useItemOnItem("TIN ORE POWDER", "TIN WITH CUPRIC ORE")
			Inventory.contains(TIN_WITH_ALL_ORE) -> useItemOnObject("TIN WITH ALL ORE", "Bunsen burner")
			Inventory.contains(TIN_FINAL_STAGE) -> useItemOnItem("Bronze wire", "Tin")
			else -> {
				log.error("No action taken")
				false
			}
		}
	}

	private fun useItemOnObject(itemName: String, objectName: String): Boolean {
		val item = findItem(itemName)
		val obj = GameObjects.newQuery().names(objectName).results().firstOrNull()
		return if (item != null && obj != null) {
			item.useOn(obj)
			Execution.delayUntil({ !item.isValid }, 2400)
		} else false
	}

	private fun useItemOnItem(itemName1: String, itemName2: String): Boolean {
		val item1 = findItem(itemName1)
		val item2 = findItem(itemName2)
		return if (item1 != null && item2 != null) {
			item1.useOn(item2)
			Execution.delayUntil({ !item1.isValid }, 2400)
		} else false
	}

	private fun leaveRoom(): Boolean {
		if (Players.getLocal()?.position != Coordinate(2478, 4940, 0)) {
			val openDoor = GameObjects.newQuery().names("Open Door").actions("Walk-through").results().firstOrNull()
			openDoor?.let { door ->
				door.interact("Walk-through")
				Delay.until({ door.distanceToPlayer()?.let { it <= 1 } == true }, 2400)
			}
			return true
		}
		val door = GameObjects.newQuery().names("Door").results().firstOrNull()
		door?.let {
			it.interact("Open")
			Delay.until({ !door.isVisible }, 2400)
		}
		return true
	}

	private fun findItem(name: String) =
		Inventory.getItems().find { it.name?.equals(name, ignoreCase = true) == true }

	private fun hasItem(name: String) = findItem(name) != null

	companion object {
		private const val METAL_SPADE_HANDLE = 5587
		private const val TIN_FULL_OF_GYPSUM = 5593
		private const val TIN_WITH_KEY_PRINT = 5594
		private const val TIN_WITH_CUPRIC_ORE = 5596
		private const val TIN_WITH_ALL_ORE = 5597
		private const val TIN_FINAL_STAGE = 5598
	}
}
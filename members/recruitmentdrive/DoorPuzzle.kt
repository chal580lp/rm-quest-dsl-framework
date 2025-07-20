package com.runemate.rebirther.quest.members.recruitmentdrive

import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.local.hud.interfaces.Interfaces

class DoorPuzzle {
	private val log = getLogger("DoorPuzzle")
	private val SLOT_IDS = mapOf(
		1 to 43,
		2 to 44,
		3 to 45,
		4 to 46
	)

	private val ARROW_IDS = mapOf(
		1 to Pair(47, 48),
		2 to Pair(49, 50),
		3 to Pair(51, 52),
		4 to Pair(53, 54)
	)

	private var lastDirection: String = ""
	private var sameDirectionCount = 0

	fun interactWithPuzzle(targetWord: String): Boolean {
		require(targetWord.length == 4) { "Target word must be exactly 4 characters long" }

		for (slot in 1..4) {
			val currentChar = getCurrentChar(slot)
			val targetChar = targetWord[slot - 1]

			log.info("Slot $slot: Current char is '$currentChar', target char is '$targetChar'")

			if (currentChar != targetChar) {
				var arrowToPress = determineArrowToPress(currentChar, targetChar)

				val pressesLeft = calculatePressesLeft(currentChar, targetChar)
				log.info("Slot $slot: Pressing $arrowToPress arrow. $pressesLeft presses left to reach '$targetChar'")

				pressArrow(slot, arrowToPress)
				return false  // Continue solving
			}
		}

		// Verify all slots are correct before declaring puzzle solved
		if (verifyAllSlots(targetWord)) {
			log.info("Puzzle solved. Final word: $targetWord")
			Interfaces.newQuery().textContains("E N T E R").results().first()?.let {
				it.click()
				Delay.delayTicks(2)
			} ?: log.warn("Failed to find 'ENTER' button")
			return true
		}

		return false
	}

	private fun verifyAllSlots(targetWord: String): Boolean {
		for (slot in 1..4) {
			if (getCurrentChar(slot) != targetWord[slot - 1]) {
				return false
			}
		}
		return true
	}

	private fun getCurrentChar(slot: Int): Char {
		var attempts = 0
		var lastChar: Char? = null
		while (attempts < 5) {
			val widget = Interfaces.getAt(WIDGET_GROUP_ID, SLOT_IDS[slot] ?: return ' ')
			val char = widget?.text?.firstOrNull() ?: ' '
			if (char != ' ' && char != lastChar) {
				return char
			}
			lastChar = char
			attempts++
		}
		log.warn("Failed to get current char for slot $slot after 5 attempts")
		return ' '
	}

	private fun determineArrowToPress(current: Char, target: Char): String {
		val currentPos = current - 'A'
		val targetPos = target - 'A'
		val clockwise = Math.floorMod(targetPos - currentPos, 26)
		val counterClockwise = Math.floorMod(currentPos - targetPos, 26)
		return if (clockwise <= counterClockwise) "Right" else "Left"
	}

	private fun calculatePressesLeft(current: Char, target: Char): Int {
		val currentPos = current - 'A'
		val targetPos = target - 'A'
		return Math.min(
			Math.floorMod(currentPos - targetPos, 26),
			Math.floorMod(targetPos - currentPos, 26)
		)
	}

	private fun pressArrow(slot: Int, direction: String) {
		val arrowPair = ARROW_IDS[slot] ?: return
		val arrowId = if (direction == "Right") arrowPair.second else arrowPair.first
		val widget = Interfaces.getAt(WIDGET_GROUP_ID, arrowId)
		widget?.click()
		Delay.delayTicks(2)
	}

	companion object {
		const val WIDGET_GROUP_ID = 285
	}
}
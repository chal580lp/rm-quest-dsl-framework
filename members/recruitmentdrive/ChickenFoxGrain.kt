package com.runemate.rebirther.quest.members.recruitmentdrive

import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.corev2.utility.VarbitRequirement
import com.runemate.game.api.hybrid.local.hud.interfaces.Equipment
import com.runemate.game.api.script.Execution
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.SIR_SPISHYUS
import com.runemate.rebirther.util.ObjectUtil.interactWithObject
import java.util.regex.Pattern

class ChickenFoxGrain {
	private val log = getLogger("ChickenFoxGrain")
	val chickenFoxGrain = Pattern.compile("Chicken|Fox|Grain")


	fun execute() {
		if (isComplete()) {
			handleCompletion()
			return
		}

		removeItemFromEquipment()

		when {
			chickenOnRightSide.check() && foxNotOnLeftSide.check() -> moveItem("Chicken", true)
			chickenOnLeftSide.check() && foxOnRightSide.check() -> moveItem("Fox", true)
			chickenOnLeftSide.check() && foxOnLeftSide.check() -> moveItem("Chicken", false)
			chickenOnRightSide.check() && grainOnRightSide.check() && foxOnLeftSide.check() -> moveItem("Grain", true)
			foxOnLeftSide.check() && grainOnLeftSide.check() && chickenOnRightSide.check() -> moveItem("Chicken", true)
		}
	}

	fun isComplete(): Boolean =
		chickenOnLeftSide.check() && foxOnLeftSide.check() && grainOnLeftSide.check() && !SIR_SPISHYUS.isReachable()

	private fun handleCompletion() {
		if (SIR_SPISHYUS.isReachable()) {
			crossBridge("Wrong Side")
		}
		log.debug("Has been completed")
		Execution.delay(1000)
	}

	private fun removeItemFromEquipment() {
		Equipment.getItems(chickenFoxGrain).firstOrNull()?.let {
			it.interact("Remove")
			Execution.delayUntil({ !Equipment.contains(chickenFoxGrain) }, 2400)
		}
	}

	private fun moveItem(itemName: String, isOnRightSide: Boolean) {
		val currentSide = SIR_SPISHYUS.isReachable()
		if (currentSide != isOnRightSide) {
			crossBridge("Wrong Side")
		}
		pickUpObject(itemName)
		if (!Equipment.contains(itemName)) {
			log.warn("Failed to pick up $itemName")
			return
		}
		crossBridge(itemName)
		removeItemFromEquipment(itemName)
	}

	private fun pickUpObject(itemName: String) {
		log.debug("Picking up $itemName")
		interactWithObject(itemName, "Pick-up")
		Execution.delayUntil({ Equipment.contains(itemName) }, 2400)
	}

	private fun crossBridge(itemName: String?) {
		log.debug("Crossing bridge with $itemName")
		interactWithObject("Precarious bridge", "Cross")
		Delay.whilst(Delay.MOVING_OR_ACTIVE)
		Delay.delayTicks(1)
	}

	private fun removeItemFromEquipment(itemName: String) {
		log.debug("Removing $itemName from equipment")
		Delay.until(Delay.NOT_MOVING)
		Equipment.getItems(itemName).firstOrNull()?.let {
			it.interact("Remove")
			Execution.delayUntil({ !Equipment.contains(itemName) }, 2400)
		}
	}

	private val foxOnRightSide = VarbitRequirement(680, 0)
	private val foxOnLeftSide = VarbitRequirement(681, 1)
	private val foxNotOnLeftSide = VarbitRequirement(681, 0)
	private val chickenOnRightSide = VarbitRequirement(682, 0)
	private val chickenOnLeftSide = VarbitRequirement(683, 1)
	private val grainOnRightSide = VarbitRequirement(684, 0)
	private val grainOnLeftSide = VarbitRequirement(685, 1)

}
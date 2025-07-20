package com.runemate.rebirther.quest.framework

import com.runemate.rebirther.baseclasses.Loadout
import com.runemate.rebirther.baseclasses.RequiredItem

data class QuestStep(
	val name: String = "",
	val value: Int,
	val entry: () -> Boolean = { true },
	val items: List<RequiredItem> = emptyList(),
	val fallback: (() -> Unit)? = null,
	val operation: Operation? = null,
	val operations: List<Operation>? = null,
	val food: Int = 0,
	val loadout: Loadout? = null,
) {
	init {
		require(operation != null || operations != null) {
			"QuestStep must have either a single act or a list of acts"
		}
	}
}

enum class QuestStepState {
	VERIFY_ITEMS,
	CHECK_CONDITION,
	EXECUTE_ACTION,
	COMPLETE
}
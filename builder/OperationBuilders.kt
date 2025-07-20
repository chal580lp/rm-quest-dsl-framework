package com.runemate.rebirther.quest.builder

import com.runemate.corev2.small.Location
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.entities.GameObject
import com.runemate.game.api.hybrid.entities.GroundItem
import com.runemate.game.api.hybrid.entities.Npc
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory
import com.runemate.game.api.hybrid.local.hud.interfaces.SpriteItem
import com.runemate.game.api.hybrid.location.Area
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.hybrid.region.GroundItems
import com.runemate.game.api.hybrid.region.Npcs
import com.runemate.rebirther.baseclasses.RequiredItem
import com.runemate.rebirther.quest.builder.QueryBuilder.buildQuery
import com.runemate.rebirther.quest.framework.Operation

class CombatBuilder : BaseOperationBuilder() {
	lateinit var target: String
	var action: String = "Attack"

	fun target(init: () -> String) {
		target = init()
	}

	fun action(init: () -> String) {
		action = init()
	}
}


class OperationBuilder : BaseOperationBuilder() {
	var requiredItems: List<RequiredItem> = emptyList()
	var action: (() -> Unit)? = null
	private val log = getLogger("OperationBuilder")

	fun action(condition: () -> Unit) {
		action = condition
	}

	fun build(): Operation = Operation(
		name = name,
		entry = {
			val entryResult = entry()
			if (entryResult) log.debug("Operation entry for '$name': $entryResult")
			entryResult
		},
		items = requiredItems,
		action = action ?: { log.error("Operation action is not defined for '$name'") },
		fallback = fallback,
	)
}

class ChapterBuilder : BaseOperationBuilder() {
	private val operations = mutableListOf<Operation>()
	private var chapterRequiredItems: List<RequiredItem> = emptyList()
	private val log = getLogger("ChapterBuilder")

	fun build(): List<Operation> = operations.map { operation ->
		operation.copy(
			name = "$name: ${operation.name}",
			entry = {
				val chapterEntry = entry()
				val operationEntry = operation.entry()
				log.debug("Chapter entry: $chapterEntry, Operation entry: $operationEntry")
				chapterEntry && operationEntry
			},
			items = mergeRequiredItems(operation.items, chapterRequiredItems)
		)
	}

	private fun mergeRequiredItems(
		operationItems: List<RequiredItem>,
		chapterItems: List<RequiredItem>,
	): List<RequiredItem> {
		val mergedItems = operationItems.toMutableList()

		chapterItems.forEach { chapterItem ->
			val existingItem = mergedItems.find { it.itemDetail == chapterItem.itemDetail }
			if (existingItem != null) {
				mergedItems[mergedItems.indexOf(existingItem)] =
					existingItem.copy(quantity = existingItem.quantity + chapterItem.quantity)
			} else {
				mergedItems.add(chapterItem)
			}
		}
		return mergedItems
	}
}

class ConversationBuilder : BaseOperationBuilder() {
	var action: String = "Talk-to"
	var with: String = ""
	var options: List<String> = emptyList()

	fun action(init: () -> String) {
		action = init()
	}

	fun with(init: () -> String) {
		with = init()
	}

	fun options(init: OptionsBuilder.() -> Unit) {
		options = OptionsBuilder().apply(init).build()
	}
}

class OptionsBuilder {
	private val optionsList = mutableListOf<String>()

	operator fun String.unaryPlus() {
		optionsList.add(this)
	}

	fun build(): List<String> = optionsList.toList()
}

class NavigationBuilder : BaseOperationBuilder() {
	lateinit var location: Location
	var teleports: Boolean = true

	fun location(init: () -> Any) {
		val result = init()
		location = when (result) {
			is Coordinate -> Location.CoordinateLocation(result)
			is Area -> Location.AreaLocation(result)
			else -> throw IllegalArgumentException("location must be either Coordinate or Area")
		}
	}
}

class InteractionBuilder : BaseOperationBuilder() {
	lateinit var action: String
	private var targetBuilder: TargetBuilder? = null
	private val conditions = mutableSetOf<QueryCondition>()

	fun action(init: () -> String) {
		action = init()
	}

	fun with(init: TargetBuilder.() -> Unit) {
		targetBuilder = TargetBuilder().apply(init)
	}

	fun conditions(init: ConditionBuilder.() -> Unit) {
		ConditionBuilder().apply(init)
	}

	fun build(): Any? {
		return targetBuilder?.build()
	}

	inner class ConditionBuilder {
		operator fun QueryCondition.unaryPlus() {
			conditions.add(this)
		}
	}

	inner class TargetBuilder {
		private var objectBuilder: (() -> GameObject?)? = null
		private var npcBuilder: (() -> Npc?)? = null
		private var groundItemBuilder: (() -> GroundItem?)? = null
		private var spriteItemBuilder: (() -> SpriteItem?)? = null
		private var itemName: String? = null

		fun sceneObj(
			named: String,
			on: Coordinate? = null,
			addEntryCondition: Boolean = true,
		) {
			objectBuilder = {
				buildQuery(named, on, conditions, action).results().nearest()
			}

			if (addEntryCondition) {
				val originalEntry = entry
				entry = {
					originalEntry() && buildQuery(named, on, conditions, action).results().isNotEmpty()
				}
			}
		}

		fun sceneNpc(named: String, on: Coordinate? = null) {
			npcBuilder = {
				val query = Npcs.newQuery().names(named)
				on?.let { query.on(it) }
				query.results().nearest()
			}
		}

		fun groundItem(named: String, on: Coordinate? = null) {
			groundItemBuilder = {
				val query = GroundItems.newQuery().names(named).surroundingsReachable()
				on?.let { query.on(it) }
				query.results().nearest()
			}
		}

		fun invItem(named: String) {
			itemName = named
			spriteItemBuilder = {
				Inventory.newQuery().names(named).results().first()
			}
			val originalEntry = entry
			entry = {
				originalEntry() && Inventory.contains(named)
			}
		}

		fun build(): Any? {
			return objectBuilder?.invoke() ?: npcBuilder?.invoke() ?: spriteItemBuilder?.invoke()
			?: groundItemBuilder?.invoke()
		}
	}
}
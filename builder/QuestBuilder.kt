package com.runemate.rebirther.quest.builder

import com.runemate.corev2.item.BaseItem
import com.runemate.corev2.item.Food
import com.runemate.game.api.hybrid.local.Skill
import com.runemate.game.api.hybrid.location.Area
import com.runemate.rebirther.baseclasses.Loadout
import com.runemate.rebirther.baseclasses.RequiredItem
import com.runemate.rebirther.baseclasses.RequiredItem.*
import com.runemate.rebirther.baseclasses.RequiredItem.NotedType.UNNOTED
import com.runemate.rebirther.baseclasses.RequiredItem.StorageType.*
import com.runemate.rebirther.details.Def
import com.runemate.rebirther.details.item.ItemDetail
import com.runemate.rebirther.details.npc.NPCDatabase
import com.runemate.rebirther.details.npc.NPCDatabase.QuestNPCDetail
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.other.Requirement
import com.runemate.rebirther.quest.framework.QuestReward

enum class QuestValueType {
	VARP, VARBIT
}
// Abstract class for Quest Properties
abstract class QuestProperties {

	abstract val name: String
	abstract val startRegion: GameRegion
	abstract val startLocation: Area
	abstract val reward: QuestReward


	abstract val description: String
	abstract val beta: Boolean
	abstract val requirements: Set<Requirement>
	abstract val questNPCs: List<QuestNPCDetail>
	abstract val monsterNPCs: List<NPCDatabase.MonsterNPCDetail>
	abstract val valueType: QuestValueType
	abstract val questValue: Int
	abstract val completionValue: Int
	abstract val requiredItems: List<RequiredItem>
	abstract val teleportItems: List<RequiredItem>
	abstract val food: Food?
	abstract val loadout: Loadout?
	abstract val skipCheck: Boolean
}

// QuestBuilder class with all necessary properties
class QuestBuilder {
	// Lateinit variables
	lateinit var name: String
	lateinit var startRegion: GameRegion
	lateinit var startLocation: Area
	lateinit var reward: QuestReward

	// Initialized variables with default values
	var description: String = ""
	var beta: Boolean = false
	var requirements: Set<Requirement> = emptySet()
	var questNPCs: List<QuestNPCDetail> = emptyList()
	var monsterNPCs: List<NPCDatabase.MonsterNPCDetail> = emptyList()
	var valueType: QuestValueType = QuestValueType.VARP
	var questValue: Int = 0
	var completionValue: Int = 0
	var requiredItems: List<RequiredItem> = emptyList()
	var teleportItems: List<RequiredItem> = emptyList()
	var food: Food? = null
	var loadout: Loadout? = null
	var skipCheck: Boolean = false

	// Build function to create QuestProperties object
	fun build(): QuestProperties {
		return object : QuestProperties() {
			override val name = this@QuestBuilder.name
			override val startRegion = this@QuestBuilder.startRegion
			override val startLocation = this@QuestBuilder.startLocation
			override val reward = this@QuestBuilder.reward
			override val description = this@QuestBuilder.description
			override val beta = this@QuestBuilder.beta
			override val requirements = this@QuestBuilder.requirements
			override val questNPCs = this@QuestBuilder.questNPCs
			override val monsterNPCs = this@QuestBuilder.monsterNPCs
			override val valueType = this@QuestBuilder.valueType
			override val questValue = this@QuestBuilder.questValue
			override val completionValue = this@QuestBuilder.completionValue
			override val requiredItems = this@QuestBuilder.requiredItems
			override val teleportItems = this@QuestBuilder.teleportItems
			override val food = this@QuestBuilder.food
			override val loadout = this@QuestBuilder.loadout
			override val skipCheck = this@QuestBuilder.skipCheck
		}
	}
}

// Extension functions for QuestBuilder

fun quest(block: QuestBuilder.() -> Unit): QuestProperties {
	val builder = QuestBuilder()
	builder.block()
	return builder.build()
}

fun QuestBuilder.info(
	name: String,
	description: String,
	startRegion: GameRegion,
	startLocation: Area,
	valueType: QuestValueType = QuestValueType.VARP,
	questValue: Int,
	completionValue: Int,
	skipCheck: Boolean = false,
	beta: Boolean = false,
) {
	this.name = name
	this.description = description
	this.startRegion = startRegion
	this.startLocation = startLocation
	this.valueType = valueType
	this.questValue = questValue
	this.completionValue = completionValue
	this.skipCheck = skipCheck
	this.beta = beta
}

fun QuestBuilder.reward(
	questPoints: Int = 0,
	experience: Map<Skill, Int> = emptyMap(),
	items: Set<BaseItem> = emptySet(),
	kudos: Int? = null,
	other: Set<String> = emptySet(),
) {
	reward = QuestReward(questPoints, experience, items, other, kudos)
}

fun QuestBuilder.combat(
	food: Food,
	loadout: Loadout? = null,
) {
	this.food = food
	this.loadout = loadout
}

fun QuestBuilder.requirement(requirement: Requirement) {
	requirements = requirements + requirement
}

fun QuestBuilder.questNPC(npc: QuestNPCDetail) {
	questNPCs = questNPCs + npc
}

fun QuestBuilder.monsterNPC(npc: NPCDatabase.MonsterNPCDetail) {
	monsterNPCs = monsterNPCs + npc
}

fun QuestBuilder.items(block: Items.() -> Unit) {
	Items().apply(block)
}

fun QuestBuilder.npcs(block: QuestNpcs.() -> Unit) {
	QuestNpcs().apply(block)
}

fun QuestBuilder.rewardQuestPoints(points: Int) {
	reward = reward.copy(questPoints = points)
}

fun QuestBuilder.rewardExperience(skill: Skill, amount: Int) {
	reward = reward.copy(experience = reward.experience + (skill to amount))
}

fun QuestBuilder.food(food: Food) {
	this.food = food
}

fun QuestBuilder.loadout(loadout: Loadout) {
	this.loadout = loadout
}

// Nested extension classes for handling items and NPCs

class Items {
	fun QuestBuilder.inventory(
		item: ItemDetail,
		quantity: Int = 1,
		withdraw: Int = quantity,
		buy: Int = withdraw,
		acquisition: List<AcquisitionType> = RequiredItem.GE,
		storage: StorageType = EITHER,
		noted: NotedType = UNNOTED,
		resupply: Boolean = true,
		alwaysRestock: Boolean = false,
	) {
		requiredItems = requiredItems + RequiredItem(
			item,
			quantity,
			withdraw,
			buy,
			storage,
			noted,
			resupply,
			acquisition,
			alwaysRestock
		)
	}

	fun QuestBuilder.equipment(
		item: ItemDetail,
		quantity: Int = 1,
		withdraw: Int = quantity,
		buy: Int = withdraw,
		acquisition: List<AcquisitionType> = RequiredItem.GE,
		storage: StorageType = EQUIPPED,
		noted: NotedType = UNNOTED,
		resupply: Boolean = true,
		alwaysRestock: Boolean = false,
	) {
		requiredItems = requiredItems + RequiredItem(
			item,
			quantity,
			withdraw,
			buy,
			storage,
			noted,
			resupply,
			acquisition,
			alwaysRestock
		)
	}

	fun QuestBuilder.bank(
		item: ItemDetail,
		quantity: Int = 1,
		withdraw: Int = quantity,
		buy: Int = withdraw,
		acquisition: List<AcquisitionType> = RequiredItem.GE,
		storage: StorageType = BANK,
		noted: NotedType = UNNOTED,
		resupply: Boolean = true,
		alwaysRestock: Boolean = false,
	) {
		requiredItems = requiredItems + RequiredItem(
			item,
			quantity,
			withdraw,
			buy,
			storage,
			noted,
			resupply,
			acquisition,
			alwaysRestock
		)
	}

	fun QuestBuilder.teleport(
		item: ItemDetail,
		quantity: Int = 1,
		withdraw: Int = quantity,
		buy: Int = withdraw,
		acquisition: List<AcquisitionType> = RequiredItem.GE,
		storage: StorageType = EITHER,
		noted: NotedType = UNNOTED,
		resupply: Boolean = true,
		alwaysRestock: Boolean = true,
	) {
		teleportItems = teleportItems + RequiredItem(
			item,
			quantity,
			withdraw,
			buy,
			storage,
			noted,
			resupply,
			acquisition,
			alwaysRestock
		)
	}

	fun QuestBuilder.coins(
		item: ItemDetail = Def.Items.GENERAL.COINS,
		quantity: Int,
		withdraw: Int = quantity,
		buy: Int = withdraw,
		acquisition: List<AcquisitionType> = emptyList(),
		storage: StorageType = INVENTORY,
		noted: NotedType = NotedType.EITHER,
		resupply: Boolean = true,
		alwaysRestock: Boolean = false,
	) {
		requiredItems = requiredItems + RequiredItem(
			item,
			quantity,
			withdraw,
			buy,
			storage,
			noted,
			resupply,
			acquisition,
			alwaysRestock
		)
	}
}

class QuestNpcs {
	fun QuestBuilder.friendly(npc: QuestNPCDetail) {
		questNPC(npc)
	}

	fun QuestBuilder.monster(npc: NPCDatabase.MonsterNPCDetail) {
		monsterNPC(npc)
	}
}

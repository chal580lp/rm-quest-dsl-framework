package com.runemate.rebirther.quest.framework

import com.runemate.corev2.item.BaseItem
import com.runemate.game.api.hybrid.local.Skill

data class QuestReward(
	val questPoints: Int,
	val experience: Map<Skill, Int> = emptyMap(),
	val items: Set<BaseItem> = emptySet(),
	val other: Set<String> = emptySet(),
	val kudos: Int? = null
)

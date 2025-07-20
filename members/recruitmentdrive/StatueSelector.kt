package com.runemate.rebirther.quest.members.recruitmentdrive

import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.hybrid.region.GameObjects
import com.runemate.game.api.script.Execution
import com.runemate.game.api.script.framework.listeners.EngineListener

class StatueSelector : EngineListener {

	private val statues = arrayOf(
		Statue("Unknown", Coordinate(0, 0, 0)),
		Statue("Bronze Halberd", Coordinate(2452, 4976, 0)),
		Statue("Silver Halberd", Coordinate(2452, 4979, 0)),
		Statue("Gold Halberd", Coordinate(2452, 4982, 0)),
		Statue("Bronze 2H", Coordinate(2450, 4976, 0)),
		Statue("Silver 2H", Coordinate(2450, 4979, 0)),
		Statue("Gold 2H", Coordinate(2450, 4982, 0)),
		Statue("Gold Mace", Coordinate(2456, 4982, 0)),
		Statue("Silver Mace", Coordinate(2456, 4979, 0)),
		Statue("Bronze mace", Coordinate(2456, 4976, 0)),
		Statue("Bronze axe", Coordinate(2454, 4976, 0)),
		Statue("Silver axe", Coordinate(2454, 4979, 0)),
		Statue("Gold axe", Coordinate(2454, 4972, 0))
	)

	fun clickCorrectStatue(index: Int): Boolean {
		val correctStatue = statues[index]
		val statue = GameObjects.newQuery()
			.on(correctStatue.coordinate)
			.visible()
			.results().firstOrNull()

		return statue?.click() == true && Execution.delay(1800)
	}

	private data class Statue(val name: String, val coordinate: Coordinate)
}
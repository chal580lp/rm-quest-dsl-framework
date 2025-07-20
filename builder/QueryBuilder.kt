package com.runemate.rebirther.quest.builder

import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.hybrid.queries.GameObjectQueryBuilder
import com.runemate.game.api.hybrid.region.GameObjects

object QueryBuilder {
	private val log = getLogger("QueryBuilder")

	fun buildQuery(
		named: String,
		on: Coordinate?,
		conditions: Set<QueryCondition>,
		action: String?,
	): GameObjectQueryBuilder {
		var query = GameObjects.newQuery().names(named)

		if (QueryCondition.COORDINATE_OFF !in conditions && on != null) {
			query = query.on(on)
		}
		if (QueryCondition.ACTIONS in conditions && action != null) {
			query = query.actions(action)
		}
		if (QueryCondition.SURROUNDINGS_REACHABLE in conditions) {
			query = query.surroundingsReachable()
		}
		if (QueryCondition.REACHABLE in conditions) {
			query = query.reachable()
		}

		return query
	}
}
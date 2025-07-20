package com.runemate.rebirther.quest.builder

import com.runemate.corev2.extensions.isTarget
import com.runemate.corev2.extensions.isTargetingPlayer
import com.runemate.corev2.small.Location
import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.game.api.hybrid.entities.GameObject
import com.runemate.game.api.hybrid.entities.GroundItem
import com.runemate.game.api.hybrid.entities.Npc
import com.runemate.game.api.hybrid.local.hud.interfaces.ChatDialog
import com.runemate.game.api.hybrid.local.hud.interfaces.SpriteItem
import com.runemate.game.api.hybrid.region.Npcs
import com.runemate.game.api.script.Execution
import com.runemate.rebirther.baseclasses.RequiredItem
import com.runemate.rebirther.handler.NpcInteractionHandler.Companion.processDialog
import com.runemate.rebirther.handler.NpcInteractionHandler.Companion.talkToNPC
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.quest.framework.Operation
import com.runemate.rebirther.quest.framework.QuestStep

abstract class BaseOperationBuilder {
	var name: String = ""
	var entry: () -> Boolean = { true }
	var exit: (() -> Unit)? = null
	var fallback: (() -> Unit)? = null

	fun name(init: () -> String) {
		name = init()
	}

	fun entry(condition: () -> Boolean) {
		entry = condition
	}

	fun exit(unit: () -> Unit) {
		exit = unit
	}

	fun fallback(init: () -> Unit) {
		fallback = init
	}
}




fun questSteps(init: QuestStepsBuilder.() -> Unit): List<QuestStep> =
	QuestStepsBuilder().apply(init).build()

class QuestStepsBuilder {
	private val steps = mutableListOf<QuestStep>()

	fun step(varpValue: Int, init: QuestStepBuilder.() -> Unit) {
		steps.add(QuestStepBuilder(varpValue).apply(init).build())
	}

	fun build(): List<QuestStep> = steps
}

class QuestStepBuilder(private val varpValue: Int) : BaseOperationBuilder() {
	private val log = getLogger("QuestStepBuilder")
	private var items: List<RequiredItem> = emptyList()
	private val operations = mutableListOf<Operation>()

	fun items(init: () -> List<RequiredItem>) {
		items = init()
	}

	fun build(): QuestStep = QuestStep(
		name = name,
		value = varpValue,
		entry = entry,
		items = items,
		fallback = fallback,
		operations = operations
	)

	fun chapter(init: ChapterBuilder.() -> Unit) {
		val chapterBuilder = ChapterBuilder().apply(init)
		operations.addAll(chapterBuilder.build())
	}

	fun operation(init: OperationBuilder.() -> Unit) {
		operations.add(OperationBuilder().apply(init).build())
	}

	fun conversation(init: ConversationBuilder.() -> Unit) {
		val builder = ConversationBuilder().apply(init)
		operation {
			name = builder.name
			entry = builder.entry
			fallback = builder.fallback
			exit = builder.exit
			action = {
				val name = builder.with
				val options = builder.options
				if (name != "") {
					talkToNPC(name, *options.toTypedArray())
				} else {
					if (!ChatDialog.isOpen()) {
						log.error("Chat dialog is not open in conversation operation")
					} else {
						log.debug("Processing dialog options")
						processDialog(options.toList())
					}
				}
			}
		}
	}

	fun navigate(init: NavigationBuilder.() -> Unit) {
		val builder = NavigationBuilder().apply(init)
		operation {
			name = builder.name
			entry = builder.entry
			fallback = builder.fallback
			exit = builder.exit
			action = {
				when (val loc = builder.location) {
					is Location.CoordinateLocation -> Navigation.toCoord(loc.coordinate)
					is Location.AreaLocation -> Navigation.toArea(loc.area)
				}
			}
		}
	}

	fun interact(init: InteractionBuilder.() -> Unit) {
		val builder = InteractionBuilder().apply(init)
		operation {
			name = builder.name
			entry = builder.entry
			fallback = builder.fallback
			exit = builder.exit
			action = {
				when (val target = builder.build()) {
					is GameObject -> {
						target.interact(builder.action)
						Delay.until(Delay.MOVING_OR_ACTIVE)
						Delay.whilst(Delay.MOVING_OR_ACTIVE)
					}

					is Npc -> {
						target.interact(builder.action)
						Execution.delay(1200)
					}

					is GroundItem -> {
						target.interact(builder.action)
						Delay.until({ !target.isValid }, 1200, Delay.MOVING)
					}

					is SpriteItem -> {
						target.interact(builder.action)
						Execution.delay(1200)
					}

					else -> {
						if (target == null) {
							log.warn("Interaction target is null")
						} else {
							log.warn("Invalid Interaction target type: ${target.javaClass.simpleName}")
						}
					}
				}
			}
		}
	}

	fun combat(init: CombatBuilder.() -> Unit) {
		val builder = CombatBuilder().apply(init)
		operation {
			name = builder.name
			entry = builder.entry
			fallback = builder.fallback
			exit = builder.exit
			action = {
				val target = Npcs.newQuery().names(builder.target).reachable().results().nearest()
				target?.let {
					if (target.isTargetingPlayer() && target.isTarget()) {
						return@let
					}
					it.interact(builder.action)
					Delay.until({ target.isTargetingPlayer() && target.isTarget() }, 1200, Delay.MOVING)
				}
			}
		}
	}
}
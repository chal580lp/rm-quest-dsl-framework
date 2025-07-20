package com.runemate.rebirther.quest

import com.runemate.rebirther.Bot
import com.runemate.rebirther.baseclasses.AbstractTask

class QuestTask(private val bot: Bot, val quest: QuestDetail) : AbstractTask() {
	override val name = quest.PROPERTIES.name
	private val questHandler = QuestHandler(bot, quest)

	override fun performExecution() = questHandler.execute()
	override fun isComplete() = questHandler.isCompleted()
	override fun checkRequirements() = questHandler.checkRequirements()
	override fun onStart() = questHandler.onStart()
	override fun onFinish() {
		questHandler.onFinish()
		super.onFinish()
	}
}
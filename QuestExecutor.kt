package com.runemate.rebirther.quest

import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.rebirther.Bot
import com.runemate.rebirther.baseclasses.AbstractTask
import com.runemate.rebirther.handler.ItemManager
import com.runemate.rebirther.quest.free.*
import com.runemate.rebirther.quest.members.DruidicRitual
import com.runemate.rebirther.quest.members.FightArena
import com.runemate.rebirther.quest.members.TreeGnomeVillage
import com.runemate.rebirther.quest.members.WitchesHouse
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDrive
import com.runemate.rebirther.quest.members.waterfall.Waterfall

class QuestExecutor(private val bot: Bot) : AbstractTask() {
	private val log = getLogger("QuestExecutor")
	private val itemManager = ItemManager(bot)
	private lateinit var questHandler: QuestHandler
	private var currentQuest: Quest? = null
	private var currentQuestDetail: QuestDetail? = null

	fun assignQuest(quest: Quest) {
		currentQuest = quest
		currentQuestDetail = createQuestDetail(quest)
		questHandler = QuestHandler(bot, currentQuestDetail!!)
	}

	private fun createQuestDetail(quest: Quest): QuestDetail {
		return when (quest) {
			is Quest.Free.BlackKnightsFortress -> BlackKnightsFortress(bot)
			is Quest.Free.CooksAssistant -> CooksAssistant(bot)
			is Quest.Free.DemonSlayer -> DemonSlayer(bot)
			is Quest.Free.DoricsQuest -> DoricsQuest(bot)
			is Quest.Free.ImpCatcher -> ImpCatcher(bot)
			is Quest.Free.RestlessGhost -> RestlessGhost(bot)
			is Quest.Free.RomeoAndJuliet -> RomeoAndJuliet(bot)
			is Quest.Free.SheepShearer -> SheepShearer(bot)
			is Quest.Members.DruidicRitual -> DruidicRitual(bot)
			is Quest.Members.FightArena -> FightArena(bot)
			is Quest.Members.PriestInPeril -> PriestInPeril(bot)
			is Quest.Members.RecruitmentDrive -> RecruitmentDrive(bot)
			is Quest.Members.TreeGnomeVillage -> TreeGnomeVillage(bot)
			is Quest.Members.Waterfall -> Waterfall(bot)
			is Quest.Members.WitchesHouse -> WitchesHouse(bot)
			else -> throw IllegalArgumentException("Unknown quest type: ${quest.name}")
		}
	}

	override fun performExecution() {
		questHandler.execute()
	}

	override val name: String
		get() = currentQuest?.name ?: "No quest assigned"

	override fun isComplete(): Boolean {
		return questHandler.isCompleted()
	}

	override fun checkRequirements(): Boolean {
		return questHandler.checkRequirements()
	}

	override fun onStart() {
		TODO("Not yet implemented")
	}
}
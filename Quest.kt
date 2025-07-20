package com.runemate.rebirther.quest


sealed class Quest(open val name: String) {
	open class Free(override val name: String) : Quest(name) {
		object BlackKnightsFortress : Free("Black Knights' Fortress")
		object CooksAssistant : Free("Cook's Assistant")
		object DemonSlayer : Free("Demon Slayer")
		object DoricsQuest : Free("Doric's Quest")
		object ImpCatcher : Free("Imp Catcher")
		object RestlessGhost : Free("The Restless Ghost")
		object RomeoAndJuliet : Free("Romeo & Juliet")
		object SheepShearer : Free("Sheep Shearer")
	}

	open class Members(override val name: String) : Quest(name) {
		// Members quests
		object DruidicRitual : Members("Druidic Ritual")
		object FightArena : Members("Fight Arena")
		object PriestInPeril : Members("Priest in Peril")
		object RecruitmentDrive : Members("Recruitment Drive")
		object TreeGnomeVillage : Members("Tree Gnome Village")
		object Waterfall : Members("Waterfall Quest")
		object WitchesHouse : Members("Witch's House")

	}

	companion object {
		fun getAllQuests(): List<Quest> = Quest::class.sealedSubclasses.mapNotNull { it.objectInstance }
		fun getF2PQuests(): List<Quest> = Free::class.sealedSubclasses.mapNotNull { it.objectInstance }
		fun getMemberQuests(): List<Quest> = Members::class.sealedSubclasses.mapNotNull { it.objectInstance }
	}
}
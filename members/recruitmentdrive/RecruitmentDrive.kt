package com.runemate.rebirther.quest.members.recruitmentdrive

import com.runemate.corev2.extensions.containsPlayer
import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil
import com.runemate.corev2.utility.VarbitRequirement
import com.runemate.game.api.hybrid.local.Quest
import com.runemate.game.api.hybrid.local.Skill
import com.runemate.game.api.hybrid.local.Varbits
import com.runemate.game.api.hybrid.local.hud.interfaces.*
import com.runemate.game.api.hybrid.location.Area
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.script.framework.listeners.VarbitListener
import com.runemate.game.api.script.framework.listeners.events.VarbitEvent
import com.runemate.rebirther.Bot
import com.runemate.rebirther.details.npc.NPCDatabase
import com.runemate.rebirther.details.npc.type.QuestNPC.SIR_AMIK_VARZE
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.other.Requirement.QuestRequirement
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.*
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.MS_HYNN_TERPRETT
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.SIR_KUAM
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.SIR_LEYE
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.SIR_REN_ITCHOOD
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.SIR_TIFFY_CASHIEN
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.SIR_TINLEY
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.VARBIT_FINISHED_FIGHT_ROOM
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.VARBIT_FINISHED_PATIENCE
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.VARBIT_FINISHED_PUZZLE_ROOM
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.VARBIT_FINISHED_STATUE_ROOM
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.VARBIT_ITCHOOD_ANSWER
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.VARBIT_ITCHOOD_FINISHED_ROOM
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.VARBIT_STATUE_ANSWER
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.VARBIT_UNSTARTED_PATIENCE
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.answers
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.bridgeRoom
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.combinationRoom
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.fightRoom
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.patienceRoom
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.recipeRoom
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.riddleRoom
import com.runemate.rebirther.quest.members.recruitmentdrive.RecruitmentDriveProperties.statueRoom

object RecruitmentDriveProperties {
	val SIR_TIFFY_CASHIEN = NPCDatabase.QuestNPCDetail(
		name = "Sir Tiffy Cashien",
		ids = setOf(4687),
		gameRegion = GameRegion.FALADOR,
		location = Coordinate(2997, 3373, 0).area,
		quests = setOf(Quest.OSRS.RECRUITMENT_DRIVE)
	)

	val SIR_REN_ITCHOOD = NPCDatabase.QuestNPCDetail(
		name = "Sir Ren Itchood",
		ids = setOf(4684),
		gameRegion = GameRegion.UNMAPPED,
		location = Coordinate(2443, 4956, 0).area,
		quests = setOf(Quest.OSRS.RECRUITMENT_DRIVE)
	)

	val SIR_SPISHYUS = NPCDatabase.QuestNPCDetail(
		name = "Sir Spishyus",
		ids = setOf(4679),
		gameRegion = GameRegion.UNMAPPED,
		location = Coordinate(2488, 4973, 0).area,
		quests = setOf(Quest.OSRS.RECRUITMENT_DRIVE)
	)

	val LADY_TABLE = NPCDatabase.QuestNPCDetail(
		name = "Lady Table",
		ids = setOf(4685),
		gameRegion = GameRegion.UNMAPPED,
		location = Coordinate(2443, 4956, 0).area,
		quests = setOf(Quest.OSRS.RECRUITMENT_DRIVE)
	)

	val MISS_CHEEVERS = NPCDatabase.QuestNPCDetail(
		name = "Miss Cheevers",
		ids = setOf(4685),
		gameRegion = GameRegion.UNMAPPED,
		location = Coordinate(2469, 4940, 0).area,
		quests = setOf(Quest.OSRS.RECRUITMENT_DRIVE)
	)

	val SIR_TINLEY = NPCDatabase.QuestNPCDetail(
		name = "Sir Tinley",
		ids = setOf(4683),
		gameRegion = GameRegion.UNMAPPED,
		location = Coordinate(2475, 4958, 0).area,
		quests = setOf(Quest.OSRS.RECRUITMENT_DRIVE)
	)

	val SIR_KUAM = NPCDatabase.QuestNPCDetail(
		name = "Sir Kuam Ferentse",
		ids = setOf(4681),
		gameRegion = GameRegion.UNMAPPED,
		location = Coordinate(2457, 4965, 0).area,
		quests = setOf(Quest.OSRS.RECRUITMENT_DRIVE)
	)
	val SIR_LEYE = NPCDatabase.QuestNPCDetail(
		name = "Sir Leye",
		ids = setOf(4682),
		gameRegion = GameRegion.UNMAPPED,
		location = Coordinate(2457, 4962, 0).area,
		quests = setOf(Quest.OSRS.RECRUITMENT_DRIVE)
	)
	val MS_HYNN_TERPRETT = NPCDatabase.QuestNPCDetail(
		name = "Ms. Hynn Terprett",
		ids = setOf(4686),
		gameRegion = GameRegion.UNMAPPED,
		location = Coordinate(2451, 4938, 0).area,
		quests = setOf(Quest.OSRS.RECRUITMENT_DRIVE)
	)

	val PROPERTIES = quest {
		name = "Recruitment Drive"
		description = ""
		startRegion = GameRegion.FALADOR
		startLocation = SIR_AMIK_VARZE.location
		valueType = QuestValueType.VARBIT
		questValue = 200
		completionValue = 20
		skipCheck = true

		requirement(QuestRequirement(Quest.OSRS.BLACK_KNIGHTS_FORTRESS))
		requirement(QuestRequirement(Quest.OSRS.DRUIDIC_RITUAL))

		reward(
			questPoints = 1,
			experience = mapOf(
				Skill.PRAYER to 1000,
				Skill.AGILITY to 1000,
				Skill.HERBLORE to 1000
			),
		)
	}
	val answers = arrayOf(
		"NULL", "TIME", "FISH", "RAIN", "BITE", "MEAT", "LAST"
	)

	const val VARBIT_ITCHOOD_FINISHED_ROOM = 663
	const val VARBIT_ITCHOOD_ANSWER = 666

	const val VARBIT_FINISHED_STATUE_ROOM = 660
	const val VARBIT_STATUE_ANSWER = 667

	const val VARBIT_UNSTARTED_PATIENCE = 667
	const val VARBIT_FINISHED_PATIENCE = 662

	const val VARBIT_FINISHED_FIGHT_ROOM = 661

	const val VARBIT_FINISHED_PUZZLE_ROOM = 665
	const val VARBIT_PUZZLE_SOLUTION = 667

	// Sir Spishyus
	val bridgeRoom = Area.Rectangular(Coordinate(2490, 4977, 0), Coordinate(2470, 4967, 0))

	// Lady Table
	val statueRoom = Area.Rectangular(Coordinate(2461, 4985, 0), Coordinate(2444, 4973, 0))

	// Sir Tinley
	val patienceRoom = Area.Rectangular(Coordinate(2481, 4961, 0), Coordinate(2470, 4952, 0))

	// Sir Cheevers
	val recipeRoom = Area.Rectangular(Coordinate(2480, 4946, 0), Coordinate(2465, 4935, 0))

	// Sir Kuam Ferentse and Sir Leye
	val fightRoom = Area.Rectangular(Coordinate(2465, 4969, 0), Coordinate(2453, 4958, 0))

	// Sir Ren Itchood
	val combinationRoom = Area.Rectangular(Coordinate(2448, 4960, 0), Coordinate(2438, 4952, 0))

	// Ms Hynn Terprett
	val riddleRoom = Area.Rectangular(Coordinate(2457, 4946, 0), Coordinate(2446, 4934, 0))

}


class RecruitmentDrive(bot: Bot) : QuestDetail(bot, RecruitmentDriveProperties.PROPERTIES), VarbitListener {
	override val log = LoggerUtil.getLogger("RecruitmentDrive")
	private val doorPuzzle = DoorPuzzle()
	private var answer: String = ""
	private val statueSelector = StatueSelector()
	private var statue: Int = -1
	private val msCheevesRoom = MsCheevesRoom()
	private val chickenFoxGrain = ChickenFoxGrain()
	private val unstartedSirTinley = VarbitRequirement(VARBIT_UNSTARTED_PATIENCE, 0)
	private val finishedSirTinley = VarbitRequirement(VARBIT_FINISHED_PATIENCE, 1)
	private val finishedLadyTable = VarbitRequirement(VARBIT_FINISHED_STATUE_ROOM, 1)
	private val finishedSirKuam = VarbitRequirement(VARBIT_FINISHED_FIGHT_ROOM, 1)
	private val finishedMsHynn = VarbitRequirement(VARBIT_FINISHED_PUZZLE_ROOM, 1)

    private fun getStatue(): Int = Varbits.load(VARBIT_STATUE_ANSWER)? ?: 0


	override fun onStart() {
		super.onStart()
		bot.eventDispatcher.addListener(this)
		log.info("Starting Recruitment Drive")
	}

	override fun onFinish() {
		super.onFinish()
		bot.eventDispatcher.removeListener(this)
		log.info("Finished Recruitment Drive")
	}

	override val steps = questSteps {
		step(1) {
			conversation {
				name { "Navigate to Sir Amik Varze" }
				entry { SIR_AMIK_VARZE.isVisibleReachable() }
				with { SIR_AMIK_VARZE.name }
				options {
					+"Yes."
				}
			}
			navigate {
				name { "Navigate to Sir Amik Varze" }
				location { SIR_AMIK_VARZE.location }
			}
		}
		step(1) {
			entry { Inventory.isEmpty() && Equipment.isEmpty() }
			fallback { bankAllItems() }
			conversation {
				name { "Navigate to Sir Tiffy" }
				entry { SIR_TIFFY_CASHIEN.isVisibleReachable() }
				with { SIR_TIFFY_CASHIEN.name }
				options {
					+"Yes."
				}
			}
			navigate {
				name { "Navigate to Sir Tiffy" }
				location { SIR_TIFFY_CASHIEN.location }
			}
		}
		step(0) {

			/*
			 * Ms Hynn Terprett room
			 */
			interact {
				name { "Exit Ms Hynn Terprett room" }
				entry { riddleRoom.containsPlayer() && finishedMsHynn.check() }
				action { "Open" }
				with { sceneObj("Door") }
				exit {
					Delay.until({ !riddleRoom.containsPlayer() })
					Delay.delayTicks(2)
				}
			}
			conversation {
				name { "Talk to Ms Hynn Terprett & solve puzzle" }
				entry { riddleRoom.containsPlayer() }
				with { MS_HYNN_TERPRETT.name }
				options {
					+"The wolves."
					+"Bucket A (32 degrees)"
					+"The number of false statements here is three."
					+"Zero."
				}
			}
			/*
			 * Sir Kuam && Leye room
			 */
			interact { // Sir Kuam
				name { "Exit Sir Kuam room" }
				entry { fightRoom.containsPlayer() && finishedSirKuam.check() }
				action { "Open" }
				with { sceneObj("Door") }
				exit {
					log.debug("Waiting for player to leave Sir Kuam room")
					Delay.until({ !fightRoom.containsPlayer() })
					Delay.delayTicks(2)
				}
			}
			interact {
				name { "Equip Steel warhammer" }
				entry { Skill.ATTACK.currentLevel >= 5 && Inventory.contains("Steel warhammer") && SIR_LEYE.isValid() }
				action { "Wield" }
				with { invItem("Steel warhammer") }
			}
			interact {
				name { "Pick up Steel warhammer" }
				entry { Skill.ATTACK.currentLevel >= 5 && !Equipment.contains("Steel warhammer") && SIR_LEYE.isValid() } // if skill check returns false, lol...
				action { "Take" }
				with { sceneObj("Steel warhammer") }
			}
			combat {
				name { "Fight Sir Leye" }
				entry { fightRoom.containsPlayer() && SIR_LEYE.isValid() }
				target { SIR_LEYE.name }
			}
			conversation {
				name { "Talk to Sir Kuam" }
				entry { fightRoom.containsPlayer() }
				with { SIR_KUAM.name }
			}
			/*
			 * Sir Tinley room
			 */
			interact {
				name { "Exit Sir Tinley room" }
				entry { patienceRoom.containsPlayer() && finishedSirTinley.check() }
				action { "Open" }
				with { sceneObj("Door") }
				exit {
					Delay.until({ !patienceRoom.containsPlayer() })
					Delay.delayTicks(2)
				}
			}
			conversation {
				name { "Talk to Sir Tinley to start the timer" }
				entry { patienceRoom.containsPlayer() && unstartedSirTinley.check() }
				with { SIR_TINLEY.name }
			}
			operation {
				name { "Wait for Sir Tinley timer to finish" }
				entry { patienceRoom.containsPlayer() }
				action {
					ChatDialog.getContinue()?.let {
						it.select()
					}
					Delay.until({ finishedSirTinley.check() }, 1200)
				}
			}
			/*
			 * Sir Spishyus room
			 */
			interact {
				name { "Exit Chicken Fox Grain Room" }
				entry { chickenFoxGrain.isComplete() && bridgeRoom.containsPlayer() }
				action { "Open" }
				with { sceneObj("Door") }
				exit {
					Delay.until({ !bridgeRoom.containsPlayer() })
					Delay.delayTicks(2)
				}
			}
			operation {
				name { "Solving Chicken Fox Grain" }
				entry { bridgeRoom.containsPlayer() }
				action { chickenFoxGrain.execute() }
			}
			/*
			 * Miss Cheevers room
			 */
			operation {
				name { "Solve the puzzle " }
				entry { recipeRoom.containsPlayer() }
				action {
					msCheevesRoom.execute()
				}

			}
			/*
			 * Statue room
			 */
			interact {
				name { "Exit statue room" }
				entry { finishedLadyTable.check() && statueRoom.containsPlayer() }
				action { "Open" }
				with { sceneObj("Door") }
				exit {
					Delay.until({ !statueRoom.containsPlayer() })
					Delay.delayTicks(2)
				}
			}
			operation {
				name { "Click on the statue" }
				entry { statueRoom.containsPlayer() && getStatue() != 0 }
				action {
					statueSelector.clickCorrectStatue(getStatue())
				}
			}
			operation {
				name { "Wait for statues to move" }
				entry { statueRoom.containsPlayer() }
				action { Delay.until({ statue != -1 }, 1200) }
			}
			/*
			 * Puzzle door room
			 */
			interact {
				name { "Open puzzle door" }
				entry {
					combinationRoom.containsPlayer()
							&& answer != ""
							&& Interfaces.getAt(DoorPuzzle.WIDGET_GROUP_ID, 47) == null
				}
				action { "Open" }
				with { sceneObj("Door") }
				exit {
					Delay.until({ !combinationRoom.containsPlayer() })
					Delay.delayTicks(2)
				}
			}
			operation {
				name { "Solve puzzle door" }
				entry {
					combinationRoom.containsPlayer() && answer != ""
				}
				action { doorPuzzle.interactWithPuzzle(answer) }
			}
			conversation {
				name { "Talk to Sir Ren Itchood" }
				entry { combinationRoom.containsPlayer() }
				with { SIR_REN_ITCHOOD.name }
				options {
					+"Can I have the clue for the door?"
				}
			}
		}
	}

	override fun atStartLocation(): Boolean = SIR_AMIK_VARZE.isVisibleReachable()

	private fun bankAllItems() {
		if (Bank.isOpen() || Bank.open()) {
			if (!Inventory.isEmpty()) {
				Bank.depositInventory()
				Delay.until({ Inventory.isEmpty() })
			}
			if (!Equipment.isEmpty()) {
				Bank.depositEquipment()
				Delay.until({ Equipment.isEmpty() })
			}
		} else {
			Navigation.toNearestBank()
		}
	}

	override fun onValueChanged(event: VarbitEvent?) {
        val answerID = Varbits.load(VARBIT_ITCHOOD_ANSWER)? ?: return
		if (answerID == 0) return
		else {
			log.debug("Answer ID: $answerID")
			answer = answers[answerID]
		}
		if (event?.varbit?.id == VARBIT_ITCHOOD_ANSWER) {
			log.debug("Answer Varbit: $answerID")
			answer = answers[answerID]
		}
		if (event?.varbit?.id == VARBIT_ITCHOOD_FINISHED_ROOM) {
			log.debug("Finished room Varbit {} from {}", event.newValue, event.oldValue)
		}
		if (event?.varbit?.id == VARBIT_STATUE_ANSWER) {
			log.debug("Statue Answer Varbit: {}", event.newValue)
			statue = event.newValue
		}
	}
}
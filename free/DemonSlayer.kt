package com.runemate.rebirther.quest.free

import com.runemate.corev2.extensions.*
import com.runemate.corev2.item.BaseItem
import com.runemate.corev2.utility.Delay
import com.runemate.corev2.utility.LoggerUtil.getLogger
import com.runemate.corev2.utility.MyPlayer
import com.runemate.corev2.utility.util
import com.runemate.game.api.hybrid.RuneScape
import com.runemate.game.api.hybrid.entities.Npc
import com.runemate.game.api.hybrid.local.hud.interfaces.Bank
import com.runemate.game.api.hybrid.local.hud.interfaces.ChatDialog
import com.runemate.game.api.hybrid.local.hud.interfaces.Equipment
import com.runemate.game.api.hybrid.local.hud.interfaces.Inventory
import com.runemate.game.api.hybrid.location.Area
import com.runemate.game.api.hybrid.location.Coordinate
import com.runemate.game.api.hybrid.region.GameObjects
import com.runemate.game.api.hybrid.region.GroundItems
import com.runemate.game.api.hybrid.region.Npcs
import com.runemate.game.api.hybrid.region.Players
import com.runemate.game.api.script.Execution
import com.runemate.game.api.script.framework.listeners.ChatboxListener
import com.runemate.game.api.script.framework.listeners.events.MessageEvent
import com.runemate.rebirther.Bot
import com.runemate.rebirther.baseclasses.RequiredItem.StorageType.EQUIPPED
import com.runemate.rebirther.details.Def.Items
import com.runemate.rebirther.details.npc.type.QuestNPC
import com.runemate.rebirther.handler.ConsumablesHandler
import com.runemate.rebirther.handler.NpcInteractionHandler.Companion.talkToNPC
import com.runemate.rebirther.navigate.Navigation
import com.runemate.rebirther.other.GameRegion
import com.runemate.rebirther.quest.QuestDetail
import com.runemate.rebirther.quest.builder.*
import com.runemate.rebirther.quest.framework.Operation
import com.runemate.rebirther.quest.framework.QuestStep
import com.runemate.rebirther.util.ObjectUtil
import com.runemate.rebirther.util.ObjectUtil.openDoorAtCoordinate
import com.runemate.rebirther.util.QuestUtil.requiredItem

private object DemonSlayerProperties {
	val properties = quest {
		info(
			name = "Demon Slayer",
			description = "",
			startRegion = GameRegion.VARROCK,
			startLocation = QuestNPC.ARIS.location,
			valueType = QuestValueType.VARBIT,
			questValue = 2561,
			completionValue = 10,
			beta = true
		)
		reward(
			questPoints = 3,
			items = setOf(BaseItem("Silverlight", 2402)),
			kudos = 5
		)
		items {
			coins(quantity = 1)
			bank(Items.GENERAL.BONES, 25)
			bank(Items.CONSUMABLE.LOBSTER, 15)
			//bank(Items.EQUIPMENT.RING_OF_RECOIL)
			teleport(Items.TELEPORT.VARROCK_TELEPORT_TAB, withdraw = 5)
			teleport(Items.TELEPORT.NECKLACE_OF_PASSAGE)
			teleport(Items.TELEPORT.RING_OF_WEALTH)
		}
	}
}

class DemonSlayer(bot: Bot) : QuestDetail(bot, DemonSlayerProperties.properties), ChatboxListener {
	override val log = getLogger("DemonSlayer")
	private lateinit var consumablesHandler: ConsumablesHandler

	private val spellWords = listOf("Carlem", "Camerinthum", "Aber", "Purchai", "Gabindo")
	private val wordOrder = mutableListOf<String>()
	private var talkedToPrysin = false
	private val drainArea = Area.Rectangular(Coordinate(3225, 3499, 0), Coordinate(3227, 3494, 0))
	private var drainBlocked = false
	private val demonArea = Area.Rectangular(Coordinate(3225, 3373, 0), Coordinate(3230, 3366, 0))
	private val key2Area = Coordinate(3228, 9899, 0).area
	private val sewerEntrance = Coordinate(3236, 3460, 0)
	private var delrithEntry = true
	private val delrithEntrance = Coordinate(3228, 3363, 0)
	private val delrith: Npc? get() = Npcs.newQuery().names("Delrith").results().firstOrNull()

	//! ALL PATHING BREAKS ON HTIS COORDINATE, NO IDEA WHY
	private val arisTentCenter = Coordinate(3203, 3424, 0)
	private val outsideArisTent = Coordinate(3209, 3424, 0)


	override fun onStart() {
		log.debug("Adding Demon Slayer listeners")
		bot.eventDispatcher.addListener(this)
		consumablesHandler = ConsumablesHandler(
			minEatAt = 8,
			maxEatAtPercentage = 50,
			minHealthPercentage = 50,
		)
		bot.eventDispatcher.addListener(consumablesHandler)

	}

	override fun onFinish() {
		log.debug("Removing Demon Slayer listeners")
		bot.eventDispatcher.removeListener(this)
		bot.eventDispatcher.removeListener(consumablesHandler)
	}

	override fun navigateToStartLocation() {
		if (!GameRegion.VARROCK.isPlayerInRegion()) {
			log.debug("Walking to Varrock")
			Navigation.toRegion(PROPERTIES.startRegion)
		} else {
			log.debug("Walking to Aris's tent")
			PROPERTIES.startLocation.area?.grow(2, 2)?.let { Navigation.viaSceneToEnh(it) }
		}
	}

	override fun atStartLocation(): Boolean {
		QuestNPC.ARIS.get().let {
			return it?.isVisible == true && it.area?.isReachable == true
		}
	}

	override val steps: List<QuestStep> = listOf(
		QuestStep(
			name = "Talk to Gypsy Aris to start the quest.",
			value = 0,
			entry = { GameRegion.VARROCK.isPlayerInRegion() || RuneScape.isCutscenePlaying() },
			operations = listOf(
				Operation.continueCutscene,
				Operation(
					entry = { spellIsComplete() },
					action = { talkToSirPrysin() },
				),
				Operation(
					entry = { QuestNPC.ARIS.isVisibleReachable() },
					action = { talkToGypsy() },
					fallback = { navigateToStartLocation() }
				)
			)
		),
		QuestStep(
			name = "Talk to Sir Prysin to continue the quest.",
			value = 1,
			operation = Operation(
				action = { talkToSirPrysin() },
				fallback = { navigateToSirPrysin() }
			)
		),
		QuestStep(
			name = "Talk to Captain Rovin to continue the quest.",
			value = 2,
			operations = listOf(
				Operation(
					entry = {
						RuneScape.isCutscenePlaying() || (Inventory.contains("Silverlight") || Equipment.contains(
							"Silverlight"
						) || Bank.contains("Silverlight"))
					},
					items = listOf(
						requiredItem(Items.EQUIPMENT.SILVERLIGHT, 1, storage = EQUIPPED),
						//requiredItem(Items.EQUIPMENT.RING_OF_RECOIL, 1, storage = EQUIPPED),
						requiredItem(Items.CONSUMABLE.LOBSTER, 1, withdraw = 15),
					),
					action = { handleDemon() }
				),
				Operation(
					name = "Leave the sewers",
					entry = { GameRegion.VARROCK.SEWER.containsPlayer() },
					action = { Navigation.exitVarrockSewers() }

				),
				Operation(
					entry = { Inventory.contains(2399) || Bank.contains(2399) },
					items = listOf(
						requiredItem(Items.QUEST.SILVERLIGHT_KEY_ROVIN, 1),
						requiredItem(Items.QUEST.SILVERLIGHT_KEY_PRYSIN, 1),
						requiredItem(Items.QUEST.SILVERLIGHT_KEY_TRAIBORN, 1)
					),
					action = { talkToSirPrysin() }
				),
				Operation(
					entry = { Inventory.contains(2401) || Bank.contains(2401) },
					items = listOf(
						requiredItem(Items.GENERAL.BONES, 25),
					),
					action = { fetchThirdKey() }
				),
				Operation(
					entry = { drainBlocked },
					action = { fetchSecondKey() }
				),
				Operation(
					entry = { Inventory.contains(2400) },
					action = { floodDrain() }
				),
				Operation(
					action = { talkToCaptainRovin() },
					fallback = { navigateToCaptainRovin() }
				)
			)
		),
	)

	private fun talkToGypsy() {
		val aris = QuestNPC.ARIS.get()
		if (aris == null || !aris.isVisible) {
			navigateToStartLocation()
			return
		}
		aris.let {
			talkToNPC(
				it, listOf(
					"What is the magical incantation?",
					"Yes.",
					"So how did Wally kill Delrith?",
					"Okay, thanks. I'll do my best to stop the demon.",
					"How am I meant to fight a demon who can destroy cities?"
				)
			)
			val text = ChatDialog.getText()
			if (!spellIsComplete() && text?.contains("Camerinthum") == true) {
				trackWords(text)
			}
		}
	}

	private fun trackWords(input: String): List<String> {
		val words = input.split(Regex("\\s+"))
		words.forEach { word ->
			val cleanWord = word.replace(Regex("[^A-Za-z]"), "")
			if (spellWords.contains(cleanWord) && !wordOrder.contains(cleanWord)) {
				wordOrder.add(cleanWord)
			}
		}
		return wordOrder.toList()
	}

	fun spellIsComplete(): Boolean = wordOrder.size == spellWords.size

	fun getWordOrder(): List<String> = wordOrder.toList()

	private fun navigateToSirPrysin() {
		if (!GameRegion.VARROCK.isPlayerInRegion()) {
			Navigation.toRegion(PROPERTIES.startRegion)
		} else {
			log.debug("Walking to Sir Prysin")
			if (!Navigation.viaEnhanced(QuestNPC.SIR_PRYSIN.location)) {
				Navigation.viaScene(GameRegion.VARROCK.CASTLE_ENTRANCE)
			}
		}
	}

	private fun talkToSirPrysin() {
		if (!QuestNPC.SIR_PRYSIN.isVisibleReachable()) {
			navigateToSirPrysin()
		} else {
			QuestNPC.SIR_PRYSIN.get()?.let {
				Execution.delayUntil({ it.animationId == -1 }, 2400)
				talkToNPC(
					it, listOf(
						"Aris said I should come and talk to you.",
						"I need to find Silverlight.",
						"Well, Aris' crystal ball seems to think otherwise.",
						"So give me the keys!",
						"Can you give me your key?",
						"So what does the drain lead to?",
						"Well I'd better go key hunting."
					)
				)
				val text = ChatDialog.getText() ?: return
				if (text.contains("Well I'd better go key hunting.") || text.contains("Can you remind me where all the keys were again?")) talkedToPrysin =
					true
			}
		}
	}

	private fun navigateToCaptainRovin() {
		if (!GameRegion.VARROCK.isPlayerInRegion()) {
			Navigation.toRegion(PROPERTIES.startRegion)
		} else {
			log.debug("Walking to Captain Rovin")
			Navigation.toArea(QuestNPC.CAPTAIN_ROVIN.location)
		}
	}

	private fun talkToCaptainRovin() {
		if (!QuestNPC.CAPTAIN_ROVIN.isVisibleReachable()) {
			navigateToCaptainRovin()
		} else {
			QuestNPC.CAPTAIN_ROVIN.get()?.let {
				talkToNPC(
					it, listOf(
						"Yes I know, but this is important.",
						"There's a demon who wants to invade this city.",
						"Yes, very.",
						"It's not them who are going to fight the demon, it's me.",
						"Sir Prysin said you would give me the key.",
						"Why did he give you one of the keys then?"

					)
				)
			}
		}
	}

	private fun floodDrain() {
		when {
			!Inventory.contains("Bucket of water") -> fetchBucket()
			!drainArea.containsPlayer() -> Navigation.toArea(drainArea)
			else -> {
				val bucket = Inventory.getItems("Bucket of water").first()
				bucket?.let {
					it.interact("Use")
					Delay.until({ Inventory.isItemSelected() }, 1200)
					GameObjects.newQuery().names("Drain").results().nearest()?.interact("Use")
					Delay.until({ drainBlocked }, 1800)
				}
			}
		}
	}

	private fun fetchBucket() {
		log.debug("Fetching bucket")
		if (Inventory.contains("Bucket")) {
			val sink = GameObjects.newQuery().names("Sink").surroundingsReachable().results().nearest()
			if (sink != null && sink.isVisible) {
				Inventory.getItems("Bucket").first()?.let {
					it.interact("Use")
					Delay.until({ Inventory.isItemSelected() }, 1200)
					sink.interact("Use")
					Delay.until({ Inventory.contains("Bucket of water") }, 1200)
				}
			} else {
				Navigation.viaEnhToSceneToWeb(Coordinate(3223, 3494, 0).area)
			}

		} else {
			val bucket = GroundItems.newQuery().names("Bucket").reachable().surroundingsReachable().results().nearest()
			if (bucket != null) {
				bucket.take()
				Delay.untilInventoryContains("Bucket")
			} else {
				Navigation.viaEnhToSceneToWeb(Coordinate(3221, 3496, 1).area)
			}
		}
	}

	private fun fetchSecondKey() {
		val rustyKey = GameObjects.newQuery().names("Rusty key").visible().results().nearest()
		if (rustyKey != null) {
			rustyKey.let {
				it.interact("Take")
			}
			Delay.until({ !rustyKey.isVisible }, 2400)
			return
		}
		val playerY = Players.getLocal()?.position?.y ?: return
		if (playerY < 9000) {
			val manhole = GameObjects.newQuery().names("Manhole").actions("Open", "Climb-down").results().nearest()
			if (manhole?.isVisible == false) {
				Navigation.viaSceneToEnh(sewerEntrance.area)
			} else {
				ObjectUtil.interactWithObject("Manhole", actions = arrayOf("Open", "Climb-down"))
			}
		} else {
			Navigation.viaSceneToEnh(key2Area)
		}
	}

	private fun fetchThirdKey() {
		if (QuestNPC.WIZARD_TRAIBORN.isVisibleReachable()) {
			val wizard = QuestNPC.WIZARD_TRAIBORN.get() ?: return
			talkToNPC(
				wizard, listOf(
					"Talk about Demon Slayer.",
					"I need to get a key given to you by Sir Prysin.",
					"He's one of the King's knights.",
					"Yes please.",
					"Well, have you got any keys knocking around?",
					"I'll get the bones for you."
				)
			)
		} else {
			if (!GameRegion.WIZARDS_TOWER.isPlayerInRegion()) {
				Navigation.toRegion(GameRegion.WIZARDS_TOWER)
			} else {
				if (QuestNPC.WIZARD_TRAIBORN.isValid()) {
					openDoorAtCoordinate(Coordinate(3109, 3162, 1))
				}
				Navigation.toWizardTowerFloor(1)
			}
		}
	}

	private fun handleDemon() {
		if (util.isAutoRetaliateEnabled()) util.toggleAutoRetaliate()
		if (!spellIsComplete()) {
			talkToGypsy()
		} else {
			fightDelrithTheWorldBringer()
		}
	}

	private fun fightDelrithTheWorldBringer() {
		when {
			ChatDialog.isOpen() -> handleChatDialog()
			RuneScape.isCutscenePlaying() -> Delay.whilst({ RuneScape.isCutscenePlaying() }, 1800)
			else -> handleDelrithFight()
		}
	}

	private fun handleChatDialog() {
		log.debug("Chat dialog open")
		if (ChatDialog.getOptions().isEmpty()) {
			ChatDialog.getContinue()?.select()
		} else {
			getWordOrder().forEach { word ->
				ChatDialog.getOption(word)?.select()
				Delay.delayTicks(2)
				ChatDialog.getContinue()?.select()
				Delay.delayTicks(2)
			}
		}
	}

	private fun handleDelrithFight() {
		val delrith = delrith
		if (delrith == null) {
			if (arisTentCenter.distanceToPlayer()?.let { it <= 3 } == true) {
				Navigation.viaScene(outsideArisTent.area.grow(1, 1))
			}
			log.debug("Walking to Delrith")
			Navigation.viaSceneToEnh(delrithEntrance)
			return
		}
		if (delrithEntry) {
			GameObjects.newQuery().names("Stone table").results().nearest()?.let {
				val pos = it.position.transform(+1, -6) ?: return
				if (pos.clickToMove()) {
					delrithEntry = false
					Delay.delayTicks(5)
				}
			}
		}
		if (delrith.definition?.actions?.contains("Banish") == true) {
			delrith.interact("Banish")
			log.debug("Banishing Delrith")
			Execution.delay(500)
		}
		when {
			!delrith.isTarget() || !delrith.isTargetingPlayer() -> {
				val darkWizards = Npcs.newQuery().names("Dark wizard").results()
				val canAttack =
					darkWizards.all { it.distanceTo(Players.getLocal()) > 8 } || delrith.position?.y.let { it == MyPlayer.y }
				if (!delrith.isTarget() && (canAttack || delrith.isTargetingPlayer())) {
					log.debug("Attacking Delrith")
					delrith.interact("Attack")
					Execution.delayUntil({ delrith.isTarget() && delrith.isTargetingPlayer() }, 400, 1200)
				} else {
//					bot.ui.updateStatus("Waiting for dark wizards to move away")
				}
				if (!delrith.isTarget() && darkWizards.any { it.isTargetingPlayer() }) {
					resetInstance()
				}
			}
		}
	}

	private fun resetInstance() {
		log.debug("Moving to exit coord")
		GameObjects.newQuery().names("Stone table").results().nearest()?.let {
			val pos = it.position.transform(1, -8) ?: return
			pos.clickToMove()
			delrithEntry = true
			Delay.until({ delrithEntrance.isVisible }, 1800)
			delrithEntrance.clickToMove()
			Execution.delayUntil({ delrith != null }, 1200)
		}
	}

	override fun onMessageReceived(event: MessageEvent?) {
		if (event != null && event.message.contains("You pour the liquid down the drain.")) {
			drainBlocked = true
		}
	}
}
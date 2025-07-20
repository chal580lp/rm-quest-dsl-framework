package com.runemate.rebirther.quest.framework

import com.runemate.corev2.utility.Delay
import com.runemate.game.api.hybrid.RuneScape
import com.runemate.game.api.hybrid.local.hud.interfaces.ChatDialog
import com.runemate.rebirther.baseclasses.RequiredItem

data class Operation(
	val name: String = "",
	val entry: () -> Boolean = { true },
	val exit: (() -> Unit)? = null,
	val items: List<RequiredItem> = emptyList(),
	val action: () -> Unit,
	val fallback: (() -> Unit)? = null,
) {
	companion object {
		val continueDialog = Operation(
			name = "Continue dialog",
			entry = { ChatDialog.isOpen() || RuneScape.isCutscenePlaying() },
			action = {
				if (ChatDialog.isOpen()) {
					ChatDialog.getContinue()?.select()
					Delay.delayTicks(2)
				} else if (RuneScape.isCutscenePlaying()) {
					Delay.whilst(RuneScape::isCutscenePlaying, 2400)
				}
			}
		)
		val continueCutscene = Operation(
			name = "Continue dialog",
			entry = { RuneScape.isCutscenePlaying() },
			action = {
				if (ChatDialog.isOpen()) {
					ChatDialog.getContinue()?.select()
					Delay.delayTicks(2)
				} else if (RuneScape.isCutscenePlaying()) {
					Delay.whilst(RuneScape::isCutscenePlaying, 2400)
				}
			}
		)
	}
}
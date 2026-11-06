package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceGameLossWith
import com.wingedsheep.sdk.scripting.ReplaceLifeReductionToZero
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Reads a second life for its controller: "if you would lose the game, instead …" (Exquisite
 * Archangel) and "if your life total would be reduced to 0 or less, instead …" (Enduring Angel).
 * Either one turns the opponent's lethal turn into a reset, so the permanent is worth more than its
 * body while it stands and is the first thing a finishing opponent has to remove.
 *
 * Only a shield over its controller ("you") reads; one that saves an opponent is no shield of ours.
 */
internal object LossShields {
    fun reads(replacement: ReplacementEffect): Boolean = when (replacement) {
        is ReplaceGameLossWith -> (replacement.appliesTo as? EventPattern.PlayerLostGameEvent)?.player == Player.You
        is ReplaceLifeReductionToZero -> (replacement.appliesTo as? EventPattern.LifeLossEvent)?.player == Player.You
        else -> false
    }
}

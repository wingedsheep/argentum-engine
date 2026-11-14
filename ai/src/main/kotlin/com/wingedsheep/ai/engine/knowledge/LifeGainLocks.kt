package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.ReplaceLifeGainWith
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Reads a lock on the opponents' life gain: "your opponents can't gain life" (Archfiend of Despair,
 * Tibalt, Rakish Instigator — [PreventLifeGain]) and "if an opponent would gain life, that player
 * loses that much life instead" (Tainted Remedy — [ReplaceLifeGainWith]). Its payoff is life the
 * opponent never gains, which no board simulation sees.
 *
 * Only a lock scoped to opponents (or to the player an Aura enchants, Grievous Wound) reads, from
 * its `appliesTo` player. One over its controller
 * (Lich's "if you would gain life, draw that many cards instead") is the card's price, not a lock of
 * ours; a symmetric one (Sulfuric Vortex's "if a player would gain life, that player gains no life
 * instead") binds us as hard as them and earns no tag, like a symmetric spell tax.
 */
internal object LifeGainLocks {
    fun reads(replacement: ReplacementEffect): Boolean = when (replacement) {
        is ReplaceLifeGainWith -> locksAnOpponent(replacement.appliesTo.player)
        is PreventLifeGain -> locksAnOpponent((replacement.appliesTo as? EventPattern.LifeGainEvent)?.player)
        else -> false
    }

    /** An opponent, or the player an Aura enchants (Grievous Wound) — a curse is cast on an opponent. */
    private fun locksAnOpponent(player: Player?): Boolean =
        player in CardIntentAnalyzer.OPPONENT_PLAYERS || player == Player.EnchantedPlayer
}

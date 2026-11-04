package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.events.AmountFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player

/** Reads one-sided, unconditional damage multiplication without mistaking a symmetrical hazard for a payoff. */
internal object DamageAmplifiers {
    fun reads(replacement: ReplacementEffect): Boolean {
        val multiplier = replacement as? DoubleDamage ?: return false
        if (multiplier.restrictions.isNotEmpty()) return false
        val event = multiplier.appliesTo as? EventPattern.DamageEvent ?: return false
        // Noncombat-only engines need a damage-spell fuel model, and conditional sources need
        // a live-state reader; neither earns this unconditional battlefield premium.
        if (event.damageType != DamageType.Any || event.amount != AmountFilter.Any ||
            event.source.statePredicates.isNotEmpty() || event.source.anyOf.isNotEmpty()) return false
        if (event.source.controllerPredicate != ControllerPredicate.ControlledByYou) return false
        return event.recipient == Recipient.Any || opponentsOnly(event.recipient)
    }

    private fun opponentsOnly(recipient: Recipient): Boolean = when (recipient) {
        is Recipient.Player -> recipient.player == Player.EachOpponent
        is Recipient.Object -> recipient.filter.controllerPredicate == ControllerPredicate.ControlledByOpponent
        is Recipient.AnyOf -> recipient.options.all(::opponentsOnly)
        Recipient.AnotherPlayer -> false
    }

    fun pickValue(replacement: ReplacementEffect): Double =
        if (reads(replacement)) 0.9 * ((replacement as DoubleDamage).multiplier - 1) else 0.0
}

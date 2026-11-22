package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.effects.AmplifyDamageThisTurnEffect
import com.wingedsheep.sdk.scripting.events.AmountFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Reads one-sided, unconditional damage amplification — a multiplier ([DoubleDamage]) or a fixed
 * "that much damage plus N" bonus ([ModifyDamageAmount], Torbran, Pyromancer's Gauntlet) — without
 * mistaking a symmetrical hazard for a payoff.
 */
internal object DamageAmplifiers {
    fun reads(replacement: ReplacementEffect): Boolean {
        val scaled = when (replacement) {
            is DoubleDamage -> replacement.multiplier > 1
            // A fixed bonus only: a dynamic one needs a live-state reader, and a negative one shrinks.
            is ModifyDamageAmount -> replacement.dynamicModifier == null && replacement.modifier > 0
            else -> false
        }
        if (!scaled || replacement.restrictions.isNotEmpty()) return false
        val event = replacement.appliesTo as? EventPattern.DamageEvent ?: return false
        return readsScope(event)
    }

    /**
     * The one-shot, turn-scoped form (Insult // Injury's Insult: "if a source you control would deal
     * damage this turn, it deals double that damage instead") — the same one-sided scope as a
     * [DoubleDamage] permanent, for the rest of the turn. An additive "plus N" bonus is not a
     * multiplier and does not read here.
     */
    fun reads(effect: AmplifyDamageThisTurnEffect): Boolean =
        effect.multiplier > 1 && readsScope(effect.appliesTo)

    private fun readsScope(event: EventPattern.DamageEvent): Boolean {
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

    fun pickValue(replacement: ReplacementEffect): Double = when {
        !reads(replacement) -> 0.0
        replacement is DoubleDamage -> 0.9 * (replacement.multiplier - 1)
        // "+2" is worth less than a doubling: it adds a fixed sum per hit, not one hit's worth.
        replacement is ModifyDamageAmount -> (PLUS_DAMAGE_PICK * replacement.modifier).coerceAtMost(0.9)
        else -> 0.0
    }

    private const val PLUS_DAMAGE_PICK = 0.3
}

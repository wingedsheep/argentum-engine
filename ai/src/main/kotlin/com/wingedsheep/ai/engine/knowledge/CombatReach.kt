package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.effects.AmassEffect
import com.wingedsheep.sdk.scripting.effects.AnimateLandEffect
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.CantBlockEffect
import com.wingedsheep.sdk.scripting.effects.CantBlockGroupEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.FightEffect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.MassAnimateEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.HealDamageEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.RemoveFromCombatEffect
import com.wingedsheep.sdk.scripting.effects.SetBaseStatsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import java.util.Collections
import java.util.IdentityHashMap

/**
 * What an activated ability can do to a combat already under way, read off its effect tree — never
 * off its card's name.
 *
 * The question [com.wingedsheep.ai.engine.CombatResponses] asks of every ability an opponent's
 * board offers while the AI is scoring a fight: is this one worth simulating as their answer? The
 * reading is deliberately coarse. It only has to keep the simulation budget off abilities that
 * cannot touch a fight (draw a card, scry, gain life, mill); the simulation itself decides whether a
 * relevant one is actually worth using, so a false positive costs one engine step and a false
 * negative is a trick the AI does not see coming.
 */
internal enum class CombatReach {
    /**
     * Changes how a fight resolves once blocks are in: power and toughness either way — until end
     * of turn, as +1/+1 / -1/-1 counters or as a new base P/T — a granted keyword (first strike, deathtouch, indestructible, trample, lifelink…), damage to a creature,
     * removal, regeneration or damage prevention, taking a creature out of combat.
     */
    FIGHT,

    /**
     * Puts a new body on the battlefield — a token, an animated land or permanent. Only worth
     * anything to a defending player before blockers are declared.
     */
    BLOCKER,

    /**
     * Keeps a creature out of a block — "target creature can't block this turn", "creatures dealt
     * damage this way can't block". Only worth anything to an attacking player before blockers are
     * declared: the opponent clearing one of our blockers on their own turn, which
     * [com.wingedsheep.ai.engine.CrackBackStress] plays out against the swing back. A can't-block
     * on the ability's own source is a drawback, not a reach.
     */
    BLOCK_DENIAL;

    companion object {
        /** [IntentTag]s whose effect reshapes a fight: the analyzer's removal, pump, protection, combat-enhancement and Equipment-moving reads. */
        private val FIGHT_TAGS = setOf(
            IntentTag.REMOVAL, IntentTag.SWEEPER, IntentTag.FIGHT, IntentTag.PUMP, IntentTag.PROTECTION,
            IntentTag.COMBAT_ENHANCEMENT, IntentTag.EQUIPMENT_RELOCATION,
        )

        /** Abilities come from card definitions and grants, so the reading is cached by identity. */
        private val cache: MutableMap<ActivatedAbility, Set<CombatReach>> =
            Collections.synchronizedMap(IdentityHashMap())

        /** What [ability] can do to a fight; empty when nothing. */
        fun of(ability: ActivatedAbility): Set<CombatReach> = cache.getOrPut(ability) { of(ability.effect) }

        /** What [effect] can do to a fight; empty when nothing. */
        fun of(effect: Effect): Set<CombatReach> {
            val reach = mutableSetOf<CombatReach>()
            EffectWalker.leaves(effect).forEach { visit(it, reach) }
            // The analyzer already reads the removal shapes the switch below does not name —
            // destroy, bounce, exile, sacrifice, a -N/-N — and token makers.
            val tags = CardIntentAnalyzer.analyzeEffect(effect).tags
            if (tags.any { it in FIGHT_TAGS }) reach += FIGHT
            if (IntentTag.TOKEN_MAKER in tags) reach += BLOCKER
            return reach
        }

        private fun visit(leaf: Effect, reach: MutableSet<CombatReach>) {
            when (leaf) {
                // [EffectWalker] keeps a ForEach whole; a team pump is "for each creature you
                // control, +1/+1", so its body is what this reading needs.
                is ForEachEffect -> EffectWalker.leaves(leaf.body).forEach { visit(it, reach) }
                is ModifyStatsEffect, is GrantKeywordEffect, is DividedDamageEffect, is FightEffect,
                is RegenerateEffect, is HealDamageEffect, is PreventDamageEffect, is RemoveFromCombatEffect -> reach += FIGHT
                // "Target creature has base power and toughness 0/2 until end of turn" (Sorceress Queen),
                // "target attacking creature has base power 0" (Singing Tree), "this creature has base
                // power and toughness 3/3" (Marsh Flitter): a new base P/T reshapes a fight either way,
                // as a pump or a shrink does — which side it helps is the simulation's to find.
                is SetBaseStatsEffect -> reach += FIGHT
                // "Tap two artifacts: put two +1/+1 counters on this creature" grows a blocker the
                // same as a pump does, and keeps it grown; a -1/-1 counter shrinks one.
                is AddCountersEffect -> if (changesStats(leaf.counterType)) reach += FIGHT
                is AddDynamicCountersEffect -> if (changesStats(leaf.counterType)) reach += FIGHT
                // Amass puts +1/+1 counters on an Army already in the fight (and is read as a
                // token maker, BLOCKER, for the Army it creates when there is none).
                is AmassEffect -> reach += FIGHT
                // Damage aimed only at a player is a clock, not a combat answer.
                is DealDamageEffect -> if (leaf.target !is EffectTarget.PlayerRef) reach += FIGHT
                is AnimateLandEffect, is BecomeCreatureEffect, is MassAnimateEffect -> reach += BLOCKER
                // "Target creature can't block this turn" keeps it out of every block; "target
                // creature can't block this creature this turn" (Screeching Griffin, Kozilek's
                // Pathfinder) keeps it off one attacker, a pairing CombatMath.canBeBlockedBy reads
                // off the floating effect — both are a blocker denied once played out.
                is CantBlockEffect -> if (leaf.target != EffectTarget.Self) reach += BLOCK_DENIAL
                // "Each creature with toughness greater than its power can't block this turn"
                // (Immobilizer Eldrazi): every creature the group holds is kept out of every block.
                is CantBlockGroupEffect -> reach += BLOCK_DENIAL
                else -> Unit
            }
        }

        /** A counter that moves power or toughness (CR 122.1a): +1/+1, -1/-1, +2/+0 and kin. */
        private fun changesStats(counter: CounterType): Boolean = counter.ptDelta != null
    }
}

package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Vanishing N (CR 702.62) as pure data — the three abilities every vanishing permanent has and
 * none of them prints as separate lines.
 *
 * A card carrying `Vanishing 3` shows one keyword line plus its reminder text; the rules give it
 * three distinct abilities:
 *
 *  1. **"This permanent enters with N time counters on it."** A replacement effect, and the only
 *     one of the three that needs `N` — hence [entersWithCounters], a factory rather than a
 *     singleton. The engine reads `N` back off the card's [KeywordAbility.Numeric] at entry.
 *  2. **"At the beginning of your upkeep, if this permanent has a time counter on it, remove a
 *     time counter from it."** [upkeepCountdown]. The intervening-`if` is what CR 702.62b prints,
 *     and it also makes the ability inert once something else has drained the counters.
 *  3. **"When the last time counter is removed from this permanent, sacrifice it."**
 *     [lastCounterSacrifice].
 *
 * Parts 2 and 3 are deliberately **two abilities, not one fused countdown**. Suspend
 * ([Suspend.countdownAbility]) folds its "when the last is removed" clause into a
 * [com.wingedsheep.sdk.dsl.Effects.If] inside the upkeep trigger because
 * nothing else ever removes a suspended card's time counters. A vanishing permanent sits on the
 * battlefield where Vampire Hexmage, Hex Parasite and friends can strip its counters at instant
 * speed — and CR 702.62c says it is sacrificed *whenever* the last one leaves, not only on an
 * upkeep. Fusing them would silently ignore every off-turn removal.
 *
 * Both triggers are singletons granted by the engine to any permanent whose *projected* keywords
 * include [Keyword.VANISHING], the same shape as [Flanking.blockedByNonFlankerTrigger] and
 * [Sieges.defeatAbility]. Deriving from the projection rather than from the printed card is what
 * makes *granted* vanishing work — a token created "with vanishing 3", or a creature that gains
 * vanishing — and what makes a "loses all abilities" effect strip it.
 *
 * Fading (CR 702.30) is a near neighbour and is deliberately **not** covered here: it counts a
 * distinct fade counter type that this codebase does not have, and its third ability is "if you
 * can't remove a counter, sacrifice it" — a different rule from vanishing's, and one turn earlier.
 */
object Vanishing {

    /** "this permanent has a time counter on it" — the CR 702.62b intervening-`if` gate. */
    private val hasTimeCounter = EntityMatches(
        EffectTarget.Self,
        GameObjectFilter.Any.copy(
            statePredicates = listOf(StatePredicate.HasCounter(CounterType.TIME))
        )
    )

    /**
     * CR 702.62b — "At the beginning of your upkeep, if this permanent has a time counter on it,
     * remove a time counter from it."
     */
    val upkeepCountdown: TriggeredAbility = TriggeredAbility(
        id = AbilityId("vanishing_countdown"),
        trigger = EventPattern.StepEvent(Step.UPKEEP, Player.You),
        binding = TriggerBinding.SELF,
        activeZones = setOf(Zone.BATTLEFIELD),
        interveningIf = hasTimeCounter,
        effect = RemoveCountersEffect(CounterType.TIME, DynamicAmount.Fixed(1), EffectTarget.Self),
        descriptionOverride = "At the beginning of your upkeep, remove a time counter from " +
            "this permanent.",
    )

    /**
     * CR 702.62c — "When the last time counter is removed from this permanent, sacrifice it."
     *
     * `lastRemoved` fires it only for the removal that empties the pile, so counting 3 → 2 → 1 is
     * silent and the removal that reaches 0 sacrifices exactly once — however that removal
     * happened.
     */
    val lastCounterSacrifice: TriggeredAbility = TriggeredAbility(
        id = AbilityId("vanishing_sacrifice"),
        trigger = EventPattern.CountersRemovedEvent(
            counterType = CounterType.TIME,
            lastRemoved = true,
        ),
        binding = TriggerBinding.SELF,
        activeZones = setOf(Zone.BATTLEFIELD),
        effect = SacrificeTargetEffect(EffectTarget.Self),
        descriptionOverride = "When the last time counter is removed from this permanent, " +
            "sacrifice it.",
    )

    /**
     * CR 702.62a — "This permanent enters with N time counters on it."
     *
     * `selfOnly` because the replacement applies to the vanishing permanent itself, never to
     * other things entering under its controller.
     */
    fun entersWithCounters(n: Int): EntersWithCounters = EntersWithCounters(
        counterType = CounterType.TIME,
        count = n,
        selfOnly = true,
    )

    /**
     * The N on a card's printed `Vanishing N`, or `null` if it has none.
     *
     * Multiple instances would each set up their own counters (CR 702.62d), so the printed values
     * are summed rather than taking the first — a permanent with vanishing 2 and vanishing 3
     * enters with five time counters.
     */
    fun printedCount(cardDef: CardDefinition): Int? {
        val total = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Numeric>()
            .filter { it.keyword == Keyword.VANISHING }
            .sumOf { it.n }
        return total.takeIf { it > 0 }
    }
}

package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Suspend (CR 702.62) as a composable, content-agnostic primitive.
 *
 * Suspend is an **exile-zone** mechanic, unlike Impending / Vanishing which live on the
 * battlefield. A suspended card sits in exile with time counters; the engine drives its
 * lifecycle off a marker component rather than the card's printed abilities, so the very
 * same machinery works for a card with no printed suspend (Taigam, Master Opportunist
 * exiles the spell you cast and grants it suspend).
 *
 * Two halves make up the mechanic:
 *  - **Putting a card into suspend** — [com.wingedsheep.sdk.dsl.Effects.Suspend] is a chain
 *    (move to exile → add N time counters → mark suspended). The marker is what the engine
 *    keys on.
 *  - **Counting down and casting** — [countdownAbility] below, a single triggered ability
 *    the engine synthesizes for every exiled card carrying the suspend marker. It fires on
 *    the owner's upkeep (the trigger detector already scans exile for `activeZone == EXILE`
 *    triggers and treats exiled cards as owner-controlled), removes one time counter, and —
 *    when that empties the pile — grants haste and plays the card for free through the
 *    ordinary [CastFromCollectionWithoutPayingCostEffect] pipeline (which handles target / X
 *    selection). [CardSource.Self] feeds the card itself into that collection.
 *
 * The intervening-if [hasTimeCounter] gate means a stale marker can never re-cast: once the
 * counters are gone the trigger simply stops firing.
 */
object Suspend {

    /** Pipeline collection key the countdown uses to hand the exiled card to the free-cast step. */
    const val PLAY_COLLECTION: String = "suspend_play"

    /** "this card has a time counter on it" — used as the intervening-if gate and the cast condition. */
    private val hasTimeCounter = EntityMatches(
        EffectTarget.Self,
        GameObjectFilter.Any.copy(statePredicates = listOf(StatePredicate.HasCounter("TIME")))
    )

    /**
     * The synthesized triggered ability granted (by the engine) to any exiled card that
     * carries the suspend marker. Functions only in exile.
     */
    val countdownAbility: TriggeredAbility = TriggeredAbility(
        id = AbilityId("suspend_countdown"),
        trigger = EventPattern.StepEvent(Step.UPKEEP, Player.You),
        binding = TriggerBinding.SELF,
        activeZone = Zone.EXILE,
        // Only count down while counters remain; this also makes a leftover marker inert.
        triggerCondition = hasTimeCounter,
        effect = CompositeEffect(
            listOf(
                RemoveCountersEffect(Counters.TIME, 1, EffectTarget.Self),
                ConditionalEffect(
                    condition = NotCondition(hasTimeCounter),
                    // CR 702.62f — "they may play it without paying its mana cost." The optional
                    // play is wrapped in [MayEffect] (both faithful to the rule and the proven
                    // cast-from-exile path used by Shiko, Paragon of the Way). Haste (CR 702.62g)
                    // is pre-armed by the suspended marker, not granted here — see GrantSuspend.
                    effect = MayEffect(
                        CompositeEffect(
                            listOf(
                                GatherCardsEffect(CardSource.Self, storeAs = PLAY_COLLECTION),
                                CastFromCollectionWithoutPayingCostEffect(from = PLAY_COLLECTION),
                            )
                        ),
                        descriptionOverride = "play it without paying its mana cost",
                    ),
                ),
            )
        ),
        descriptionOverride = "At the beginning of its owner's upkeep, they remove a time " +
            "counter from this card. When the last is removed, they play it without paying " +
            "its mana cost. If it's a creature, it gains haste.",
    )
}

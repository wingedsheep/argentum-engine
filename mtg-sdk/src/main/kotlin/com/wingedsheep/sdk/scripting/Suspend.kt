package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

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
 *  - **Counting down and casting** — CR 702.62a's two exile-zone triggered abilities, which the
 *    engine synthesizes for every exiled card carrying the suspend marker:
 *    [countdownAbility] removes a time counter at the owner's upkeep, and [lastCounterCastAbility]
 *    lets the owner play the card for free when the last one is removed — through the ordinary
 *    [CastFromCollectionWithoutPayingCostEffect] pipeline (which handles target / X selection).
 *    [CardSource.Entity] feeds the card itself into that collection.
 *
 * The two are deliberately **separate abilities**, like [Vanishing]'s countdown and sacrifice:
 * "it doesn't matter why the last time counter was removed or what effect removed it" (the suspend
 * rulings), so Clockspinning or Jhoira's Timebug taking the last counter off a suspended card must
 * let its owner cast it there and then, not at their next upkeep.
 */
object Suspend {

    /** Pipeline collection key the cast trigger uses to hand the exiled card to the free-cast step. */
    const val PLAY_COLLECTION: String = "suspend_play"

    /** "this card has a time counter on it" — the countdown's intervening-if ("if this card is suspended"). */
    private val hasTimeCounter = EntityMatches(
        EffectTarget.Self,
        GameObjectFilter.Any.copy(statePredicates = listOf(StatePredicate.HasCounter(CounterType.TIME)))
    )

    /** "this card is in exile" — the cast trigger's CR 702.62a intervening-if ("if it's exiled"). */
    private val inExile = EntityMatches(
        EffectTarget.Self,
        GameObjectFilter.Any.copy(statePredicates = listOf(StatePredicate.InZone(Zone.EXILE)))
    )

    /**
     * CR 702.62a — "At the beginning of your upkeep, if this card is suspended, remove a time
     * counter from it." Granted (by the engine) to any exiled card that carries the suspend marker;
     * functions only in exile. The intervening-if [hasTimeCounter] makes a leftover marker inert.
     */
    val countdownAbility: TriggeredAbility = TriggeredAbility(
        id = AbilityId("suspend_countdown"),
        trigger = EventPattern.StepEvent(Step.UPKEEP, Player.You),
        binding = TriggerBinding.SELF,
        activeZones = setOf(Zone.EXILE),
        interveningIf = hasTimeCounter,
        effect = RemoveCountersEffect(CounterType.TIME, DynamicAmount.Fixed(1), EffectTarget.Self),
        descriptionOverride = "At the beginning of its owner's upkeep, they remove a time " +
            "counter from this card.",
    )

    /**
     * CR 702.62a — "When the last time counter is removed from this card, if it's exiled, you may
     * play it without paying its mana cost if able. If you don't, it remains exiled." Granted
     * alongside [countdownAbility]; `lastRemoved` fires it once, for whichever removal empties the
     * pile — the upkeep countdown or any other effect. Haste (CR 702.62a's last sentence) is
     * pre-armed by the suspended marker, not granted here — see GrantSuspend.
     */
    val lastCounterCastAbility: TriggeredAbility = TriggeredAbility(
        id = AbilityId("suspend_cast"),
        trigger = EventPattern.CountersRemovedEvent(
            counterType = CounterType.TIME,
            lastRemoved = true,
        ),
        binding = TriggerBinding.SELF,
        activeZones = setOf(Zone.EXILE),
        interveningIf = inExile,
        // The optional play is wrapped in [Effects.May] — the proven cast-from-exile path used by
        // Shiko, Paragon of the Way.
        effect = Effects.May(
            CompositeEffect(
                listOf(
                    GatherCardsEffect(CardSource.Entity(EffectTarget.Self), storeAs = PLAY_COLLECTION),
                    CastFromCollectionWithoutPayingCostEffect(from = PLAY_COLLECTION),
                )
            ),
            descriptionOverride = "play it without paying its mana cost",
        ),
        descriptionOverride = "When the last time counter is removed from this card, if it's " +
            "exiled, its owner may play it without paying its mana cost. If it's a creature, it " +
            "gains haste.",
    )

    /** Both synthesized suspend abilities, in the order CR 702.62a lists them. */
    val exileAbilities: List<TriggeredAbility> = listOf(countdownAbility, lastCounterCastAbility)
}

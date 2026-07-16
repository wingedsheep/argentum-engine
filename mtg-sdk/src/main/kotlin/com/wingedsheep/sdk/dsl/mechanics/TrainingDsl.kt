package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.EmitTrainedEventEffect
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reminder text for Training, rendered on every card and token that has the keyword. This is the
 * *printed* oracle reminder (shorter than the CR 702.149a rules text — "attacks with another
 * creature with greater power" rather than "and at least one other creature with power greater
 * than this creature's power attack"); it matches what's on Gryff Rider, Cloaked Cadet, Torens,
 * and the Human Soldier token, so descriptions line up with Scryfall.
 */
private const val TRAINING_REMINDER =
    "Training (Whenever this creature attacks with another creature with greater power, " +
        "put a +1/+1 counter on this creature.)"

/**
 * The single triggered ability that *is* Training (CR 702.149a): an attack trigger gated by
 * [AttackPredicate.AttackedAlongsideGreaterPower] (a projected-power comparison across the
 * attacking band) whose effect puts one +1/+1 counter on the source.
 *
 * Exposed as a standalone builder — not just inlined into [training] — so an intrinsically-training
 * **token** can carry the identical ability via [com.wingedsheep.sdk.scripting.effects.CreateTokenEffect.triggeredAbilities]
 * (e.g. Torens, Fist of the Angels' "1/1 Human Soldier creature token with training"). The token
 * additionally sets `keywords = setOf(Keyword.TRAINING)` for the display badge; this ability supplies
 * the behavior. Because each instance is a separate `TriggeredAbility`, two copies trigger separately
 * (CR 702.149b).
 *
 * The ability's effect is a two-step [CompositeEffect]: place one +1/+1 counter, then
 * [EmitTrainedEventEffect] — the CR 702.149c "when this creature trains" signal. The emit is gated on
 * the counter actually landing (see [EmitTrainedEventEffect]), so a Solemnity-type "can't have
 * counters" prohibition trains nothing and fires no [com.wingedsheep.sdk.scripting.EventPattern.TrainedEvent].
 * A card with no "when it trains" payoff (Gryff Rider, Cloaked Cadet, Torens) simply has no watcher for
 * the event; emitting it unconditionally keeps the trained signal faithful for the payoff cards
 * (Savior of Ollenbock) without any per-card wiring.
 */
fun trainingTriggeredAbility(): TriggeredAbility {
    val spec = Triggers.attacks(requires = setOf(AttackPredicate.AttackedAlongsideGreaterPower))
    return TriggeredAbility.create(
        trigger = spec.event,
        binding = spec.binding, // SELF
        effect = CompositeEffect(
            listOf(
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                EmitTrainedEventEffect
            )
        ),
        descriptionOverride = TRAINING_REMINDER
    )
}

/**
 * Add Training (CR 702.149, Innistrad: Midnight Hunt) — keyword + the triggered ability.
 *
 * "Whenever this creature and at least one other creature with power greater than this creature's
 * power attack, put a +1/+1 counter on this creature" (CR 702.149a).
 *
 * The keyword is display-only (no separate Training handler exists); the behavior is composed here
 * from existing primitives via [trainingTriggeredAbility]. Power is compared through **projected**
 * state, so an anthem or aura on the *other* attacker can flip the trigger from off to on (the
 * matcher branch for [AttackPredicate.AttackedAlongsideGreaterPower] reads
 * `state.projectedState.getPower(...)` for every attacker).
 *
 * Multiple instances trigger separately (CR 702.149b): calling this twice, or adding a second
 * [trainingTriggeredAbility], installs two independent triggers that each add a counter.
 */
fun CardBuilder.training() {
    keywordSet.add(Keyword.TRAINING)
    triggeredAbilities.add(trainingTriggeredAbility())
}

package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Costume Closet (Marvel's Spider-Man, #5)
 * {1}{W}
 * Artifact
 *
 * This artifact enters with two +1/+1 counters on it.
 * {T}: Move a +1/+1 counter from this artifact onto target creature you control. Activate only as a
 * sorcery.
 * Whenever a modified creature you control leaves the battlefield, put a +1/+1 counter on this
 * artifact. (Equipment, Auras you control, and counters are modifications.)
 *
 * Implementation (fully built from composable primitives):
 *  - Enters-with-counters: [EntersWithCounters] replacement (`selfOnly = true`, count 2) — same
 *    shape as Peter Parker's Camera / Braided Net.
 *  - Move-counter: sorcery-speed `{T}` activated ability. [Effects.MoveCounters] moves one +1/+1
 *    counter from this artifact ([EffectTarget.Self]) onto a target creature you control, capped at
 *    the number actually present.
 *  - Modified-LTB trigger: a [Triggers.leavesBattlefield] with `TriggerBinding.ANY` over a
 *    "creature you control" filter narrowed by [StatePredicate.IsModified] (has a counter, an Aura,
 *    or Equipment attached — the MTG "modified" definition). The filter is evaluated against the
 *    departing permanent's last-known information, so a creature that was modified as it left still
 *    triggers this. The reward is one +1/+1 counter on the Closet itself.
 */
val CostumeCloset = card("Costume Closet") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "This artifact enters with two +1/+1 counters on it.\n" +
        "{T}: Move a +1/+1 counter from this artifact onto target creature you control. Activate " +
        "only as a sorcery.\n" +
        "Whenever a modified creature you control leaves the battlefield, put a +1/+1 counter on " +
        "this artifact. (Equipment, Auras you control, and counters are modifications.)"

    // This artifact enters with two +1/+1 counters on it.
    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.PLUS_ONE_PLUS_ONE),
            count = 2,
            selfOnly = true
        )
    )

    // {T}: Move a +1/+1 counter from this artifact onto target creature you control. Sorcery speed.
    activatedAbility {
        cost = Costs.Tap
        timing = TimingRule.SorcerySpeed
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.MoveCounters(
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmount.Fixed(1),
            source = EffectTarget.Self,
            destination = creature
        )
        description = "{T}: Move a +1/+1 counter from this artifact onto target creature you " +
            "control. Activate only as a sorcery."
    }

    // Whenever a modified creature you control leaves the battlefield, put a +1/+1 counter on this.
    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl().let {
                it.copy(statePredicates = it.statePredicates + StatePredicate.IsModified)
            },
            binding = TriggerBinding.ANY
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever a modified creature you control leaves the battlefield, put a " +
            "+1/+1 counter on this artifact."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Bastien Grivet"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc641f4a-ddbe-4f7d-bb55-eabf11f8b7fb.jpg?1783905363"
    }
}

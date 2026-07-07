package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Explorer's Cache — The Lost Caverns of Ixalan #184
 * {1}{G} · Artifact · Uncommon
 * Artist: Nereida
 *
 * This artifact enters with two +1/+1 counters on it.
 * Whenever a creature you control with a +1/+1 counter on it dies, put a +1/+1 counter on
 *   this artifact.
 * {T}: Move a +1/+1 counter from this artifact onto target creature. Activate only as a sorcery.
 *
 * Ability 1 — [EntersWithCounters] replacement effect (count = 2, selfOnly = true) applies the
 *   two +1/+1 counters as the Cache enters the battlefield; no counterType parameter needed since
 *   the default is PlusOnePlusOne.
 *
 * Ability 2 — The dies trigger uses [Triggers.leavesBattlefield] with a
 *   `GameObjectFilter.Creature.youControl().withCounter(Counters.PLUS_ONE_PLUS_ONE)` filter
 *   (ANY binding, to = GRAVEYARD). The engine evaluates `withCounter` against last-known-information
 *   for zone-change triggers (TriggerMatcher.matchesStatePredicateForZoneChangeTrigger, CR 603.10),
 *   so the +1/+1 counter check correctly reads the dying creature's captured counters rather than
 *   its already-gone live state. No triggerCondition is needed — the filter alone guards the trigger.
 *
 * Ability 3 — [Costs.Tap] + [TargetCreature] (any creature) + [Effects.MoveCounters] with a
 *   fixed amount of 1, source = Self (the artifact), destination = ContextTarget(0) (the chosen
 *   creature). [TimingRule.SorcerySpeed] enforces "Activate only as a sorcery."
 */
val ExplorersCache = card("Explorer's Cache") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "This artifact enters with two +1/+1 counters on it.\n" +
        "Whenever a creature you control with a +1/+1 counter on it dies, put a +1/+1 counter on this artifact.\n" +
        "{T}: Move a +1/+1 counter from this artifact onto target creature. Activate only as a sorcery."

    // This artifact enters with two +1/+1 counters on it.
    replacementEffect(EntersWithCounters(count = 2, selfOnly = true))

    // Whenever a creature you control with a +1/+1 counter on it dies,
    // put a +1/+1 counter on this artifact.
    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.youControl().withCounter(Counters.PLUS_ONE_PLUS_ONE),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever a creature you control with a +1/+1 counter on it dies, " +
            "put a +1/+1 counter on this artifact."
    }

    // {T}: Move a +1/+1 counter from this artifact onto target creature. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Tap
        target = TargetCreature()
        effect = Effects.MoveCounters(
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmount.Fixed(1),
            source = EffectTarget.Self,
            destination = EffectTarget.ContextTarget(0)
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "Nereida"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86b36214-9c7e-4a24-93d4-17b2d00cbc51.jpg?1782694462"
    }
}

package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Icatian Moneychanger
 * {W}
 * Creature — Human
 * 0/2
 * This creature enters with three credit counters on it.
 * When this creature enters, it deals 3 damage to you.
 * At the beginning of your upkeep, put a credit counter on this creature.
 * Sacrifice this creature: You gain 1 life for each credit counter on this creature. Activate only
 * during your upkeep.
 *
 * The lifegain counts the credit counters the Moneychanger *had* — the sacrifice is a cost, paid
 * before the ability resolves, so the count is last-known information (CR 113.7a): three, plus one
 * per upkeep it survived.
 */
val IcatianMoneychanger = card("Icatian Moneychanger") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human"
    oracleText = "This creature enters with three credit counters on it.\n" +
        "When this creature enters, it deals 3 damage to you.\n" +
        "At the beginning of your upkeep, put a credit counter on this creature.\n" +
        "Sacrifice this creature: You gain 1 life for each credit counter on this creature. " +
        "Activate only during your upkeep."
    power = 0
    toughness = 2

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.Named(Counters.CREDIT),
            count = 3,
            selfOnly = true
        )
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DealDamage(3, EffectTarget.Controller)
        description = "When this creature enters, it deals 3 damage to you."
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.AddCounters(Counters.CREDIT, 1, EffectTarget.Self)
        description = "At the beginning of your upkeep, put a credit counter on this creature."
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.OnlyDuringYourTurn,
                ActivationRestriction.DuringStep(Step.UPKEEP)
            )
        )
        // Last-known information (CR 113.7a): the sacrifice is a *cost*, so by the time the
        // ability resolves the Moneychanger is in the graveyard with its counters stripped. Reading
        // the live entity would gain 0 life every time.
        effect = Effects.GainLife(
            DynamicAmounts.lastKnownSourceCounters(CounterTypeFilter.Named(Counters.CREDIT))
        )
        description = "Sacrifice this creature: You gain 1 life for each credit counter on this creature. Activate only during your upkeep."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10a"
        artist = "Drew Tucker"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3d502d4-4a96-47b3-ae26-8b2c9f36623d.jpg?1783947917"
    }
}

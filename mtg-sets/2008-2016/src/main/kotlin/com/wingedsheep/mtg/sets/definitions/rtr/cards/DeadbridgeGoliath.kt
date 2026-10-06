package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deadbridge Goliath {2}{G}{G}
 * Creature — Insect
 * 5/5
 *
 * Scavenge {4}{G}{G} ({4}{G}{G}, Exile this card from your graveyard: Put a number of +1/+1
 * counters equal to this card's power on target creature. Scavenge only as a sorcery.)
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Scavenge (CR 702.97) is an ordinary graveyard-activated ability: the mana plus
 * [Costs.ExileSelf] (exiling the card is part of the cost, per the 2013-04-15 ruling), sorcery
 * timing, and `+1/+1` counters equal to the source's power. By resolution the card is in exile,
 * so [DynamicAmounts.sourcePower] reads its printed power.
 */
val DeadbridgeGoliath = card("Deadbridge Goliath") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 5
    toughness = 5
    oracleText = "Scavenge {4}{G}{G} ({4}{G}{G}, Exile this card from your graveyard: Put a number of " +
        "+1/+1 counters equal to this card's power on target creature. Scavenge only as a sorcery.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{G}{G}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        val t = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.sourcePower(), t)
        description = "Scavenge {4}{G}{G}"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Chase Stone"
        flavorText = "Some Golgari insects live for centuries—and they never stop growing."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6ad03e99-25d3-4a09-819b-9192dfd8c9d2.jpg?1783940350"
        ruling(
            "2013-04-15",
            "Exiling the creature card with scavenge is part of the cost of activating the scavenge " +
                "ability. Once the ability is activated and the cost is paid, it's too late to stop the " +
                "ability from being activated by trying to remove the creature card from the graveyard.",
        )
    }
}

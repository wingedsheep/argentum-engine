package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Korozda Monitor {2}{G}{G}
 * Creature — Lizard
 * 3/3
 *
 * Trample
 * Scavenge {5}{G}{G} ({5}{G}{G}, Exile this card from your graveyard: Put a number of +1/+1
 * counters equal to this card's power on target creature. Scavenge only as a sorcery.)
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Scavenge uses the same graveyard-activated shape as Drudge Beetle.
 */
val KorozdaMonitor = card("Korozda Monitor") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Lizard"
    power = 3
    toughness = 3
    oracleText = "Trample\n" +
        "Scavenge {5}{G}{G} ({5}{G}{G}, Exile this card from your graveyard: Put a number of +1/+1 " +
        "counters equal to this card's power on target creature. Scavenge only as a sorcery.)"

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{G}{G}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        val t = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.sourcePower(), t)
        description = "Scavenge {5}{G}{G}"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f319d57-7a54-4b10-86d4-58d7b3994844.jpg?1783940348"
        ruling(
            "2013-04-15",
            "Exiling the creature card with scavenge is part of the cost of activating the scavenge " +
                "ability. Once the ability is activated and the cost is paid, it's too late to stop the " +
                "ability from being activated by trying to remove the creature card from the graveyard.",
        )
    }
}

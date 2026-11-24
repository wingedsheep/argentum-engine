package com.wingedsheep.mtg.sets.definitions.ddj.cards

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
 * Dreg Mangler {1}{B}{G}
 * Creature — Plant Zombie
 * 3/3
 *
 * Haste
 * Scavenge {3}{B}{G} ({3}{B}{G}, Exile this card from your graveyard: Put a number of +1/+1
 * counters equal to this card's power on target creature. Scavenge only as a sorcery.)
 *
 * Canonical printing: Duel Decks: Izzet vs. Golgari, the card's earliest real printing.
 *
 * Scavenge uses the same graveyard-activated shape as Drudge Beetle.
 */
val DregMangler = card("Dreg Mangler") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Plant Zombie"
    power = 3
    toughness = 3
    oracleText = "Haste\n" +
        "Scavenge {3}{B}{G} ({3}{B}{G}, Exile this card from your graveyard: Put a number of +1/+1 " +
        "counters equal to this card's power on target creature. Scavenge only as a sorcery.)"

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}{G}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        val t = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.sourcePower(), t)
        description = "Scavenge {3}{B}{G}"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9ddfb82c-3553-4d89-a57a-b16ab5e7cd73.jpg?1783940402"
        ruling(
            "2013-04-15",
            "Exiling the creature card with scavenge is part of the cost of activating the scavenge " +
                "ability. Once the ability is activated and the cost is paid, it's too late to stop the " +
                "ability from being activated by trying to remove the creature card from the graveyard.",
        )
    }
}

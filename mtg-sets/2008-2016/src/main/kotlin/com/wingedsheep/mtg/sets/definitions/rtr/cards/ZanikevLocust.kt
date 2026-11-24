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
 * Zanikev Locust {5}{B}
 * Creature — Insect
 * 3/3
 *
 * Flying
 * Scavenge {2}{B}{B} ({2}{B}{B}, Exile this card from your graveyard: Put a number of +1/+1
 * counters equal to this card's power on target creature. Scavenge only as a sorcery.)
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Scavenge uses the same graveyard-activated shape as Drudge Beetle.
 */
val ZanikevLocust = card("Zanikev Locust") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Scavenge {2}{B}{B} ({2}{B}{B}, Exile this card from your graveyard: Put a number of +1/+1 " +
        "counters equal to this card's power on target creature. Scavenge only as a sorcery.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{B}{B}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        val t = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.sourcePower(), t)
        description = "Scavenge {2}{B}{B}"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adcbd7ee-8958-46fe-abb0-e899e7d2e654.jpg?1783940358"
        ruling(
            "2013-04-15",
            "Exiling the creature card with scavenge is part of the cost of activating the scavenge " +
                "ability. Once the ability is activated and the cost is paid, it's too late to stop the " +
                "ability from being activated by trying to remove the creature card from the graveyard.",
        )
    }
}

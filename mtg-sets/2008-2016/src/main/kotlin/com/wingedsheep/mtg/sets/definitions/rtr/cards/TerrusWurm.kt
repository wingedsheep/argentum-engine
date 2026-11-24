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
 * Terrus Wurm {6}{B}
 * Creature — Zombie Wurm
 * 5/5
 *
 * Scavenge {6}{B} ({6}{B}, Exile this card from your graveyard: Put a number of +1/+1 counters
 * equal to this card's power on target creature. Scavenge only as a sorcery.)
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Scavenge uses the same graveyard-activated shape as Drudge Beetle.
 */
val TerrusWurm = card("Terrus Wurm") {
    manaCost = "{6}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Wurm"
    power = 5
    toughness = 5
    oracleText = "Scavenge {6}{B} ({6}{B}, Exile this card from your graveyard: Put a number of " +
        "+1/+1 counters equal to this card's power on target creature. Scavenge only as a sorcery.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}{B}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        val t = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.sourcePower(), t)
        description = "Scavenge {6}{B}"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Cliff Childs"
        flavorText = "\"We follow it through its tunnels, and where it dies, we farm.\"\n—Mihas, spore druid"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/1998135c-7b7f-402b-a8a5-4f4af131b1bc.jpg?1783940359"
        ruling(
            "2013-04-15",
            "Exiling the creature card with scavenge is part of the cost of activating the scavenge " +
                "ability. Once the ability is activated and the cost is paid, it's too late to stop the " +
                "ability from being activated by trying to remove the creature card from the graveyard.",
        )
    }
}

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
 * Slitherhead {B/G}
 * Creature — Plant Zombie
 * 1/1
 *
 * Scavenge {0} ({0}, Exile this card from your graveyard: Put a number of +1/+1 counters equal
 * to this card's power on target creature. Scavenge only as a sorcery.)
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Scavenge uses the same graveyard-activated shape as Drudge Beetle.
 */
val Slitherhead = card("Slitherhead") {
    manaCost = "{B/G}"
    colorIdentity = "BG"
    typeLine = "Creature — Plant Zombie"
    power = 1
    toughness = 1
    oracleText = "Scavenge {0} ({0}, Exile this card from your graveyard: Put a number of " +
        "+1/+1 counters equal to this card's power on target creature. Scavenge only as a sorcery.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{0}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        val t = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.sourcePower(), t)
        description = "Scavenge {0}"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "222"
        artist = "Greg Staples"
        flavorText = "\"It makes better use of Uncle Dragi's head than he ever did.\"\n—Pesha, retired blacksmith"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9327905-a254-4885-8310-69fc153ec52f.jpg?1783940326"
        ruling(
            "2013-04-15",
            "Exiling the creature card with scavenge is part of the cost of activating the scavenge " +
                "ability. Once the ability is activated and the cost is paid, it's too late to stop the " +
                "ability from being activated by trying to remove the creature card from the graveyard.",
        )
    }
}

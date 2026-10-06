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
 * Drudge Beetle {1}{G}
 * Creature — Insect
 * 2/2
 *
 * Scavenge {5}{G} ({5}{G}, Exile this card from your graveyard: Put a number of +1/+1 counters
 * equal to this card's power on target creature. Scavenge only as a sorcery.)
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Scavenge is the same graveyard-activated shape as Deadbridge Goliath: mana plus
 * [Costs.ExileSelf], sorcery timing, and +1/+1 counters equal to [DynamicAmounts.sourcePower].
 */
val DrudgeBeetle = card("Drudge Beetle") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    power = 2
    toughness = 2
    oracleText = "Scavenge {5}{G} ({5}{G}, Exile this card from your graveyard: Put a number of " +
        "+1/+1 counters equal to this card's power on target creature. Scavenge only as a sorcery.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{G}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        timing = TimingRule.SorcerySpeed
        val t = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.sourcePower(), t)
        description = "Scavenge {5}{G}"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Slawomir Maniak"
        flavorText = "The Street Swarm is the labor class that drives the Golgari's endless cycle of life and death."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4812e81-beca-4afc-b2f2-24d5ab27abff.jpg?1783940349"
        ruling(
            "2013-04-15",
            "Exiling the creature card with scavenge is part of the cost of activating the scavenge " +
                "ability. Once the ability is activated and the cost is paid, it's too late to stop the " +
                "ability from being activated by trying to remove the creature card from the graveyard.",
        )
    }
}

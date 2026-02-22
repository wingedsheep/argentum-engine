package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect

/**
 * Unspeakable Symbol
 * {1}{B}{B}
 * Enchantment
 * Pay 3 life: Put a +1/+1 counter on target creature.
 */
val UnspeakableSymbol = card("Unspeakable Symbol") {
    manaCost = "{1}{B}{B}"
    typeLine = "Enchantment"
    oracleText = "Pay 3 life: Put a +1/+1 counter on target creature."

    activatedAbility {
        cost = Costs.PayLife(3)
        val t = target("target creature", Targets.Creature)
        effect = AddCountersEffect(
            counterType = "+1/+1",
            count = 1,
            target = t
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Arnie Swekel"
        flavorText = "The symbols are spread throughout Aphetto, marking sites where minions of the Raven Guild and the Cabal can seek refuge."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cc4601b-5f34-4733-8c32-9779de4c502c.jpg?1562527057"
    }
}

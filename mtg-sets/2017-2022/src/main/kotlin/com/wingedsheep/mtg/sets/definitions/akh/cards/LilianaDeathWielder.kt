package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Liliana, Death Wielder — Amonkhet #275 (Planeswalker Deck)
 * {5}{B}{B} · Legendary Planeswalker — Liliana · Mythic · Starting loyalty 5
 *
 * +2: Put a -1/-1 counter on up to one target creature.
 * −3: Destroy target creature with a -1/-1 counter on it.
 * −10: Return all creature cards from your graveyard to the battlefield.
 */
val LilianaDeathWielder = card("Liliana, Death Wielder") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Liliana"
    startingLoyalty = 5
    oracleText = "+2: Put a -1/-1 counter on up to one target creature.\n" +
        "−3: Destroy target creature with a -1/-1 counter on it.\n" +
        "−10: Return all creature cards from your graveyard to the battlefield."

    // +2: Put a -1/-1 counter on up to one target creature.
    loyaltyAbility(+2) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, creature)
        description = "Put a -1/-1 counter on up to one target creature."
    }

    // −3: Destroy target creature with a -1/-1 counter on it.
    loyaltyAbility(-3) {
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.withCounter(CounterType.MINUS_ONE_MINUS_ONE))
        )
        effect = Effects.Destroy(creature)
        description = "Destroy target creature with a -1/-1 counter on it."
    }

    // −10: Return all creature cards from your graveyard to the battlefield.
    loyaltyAbility(-10) {
        effect = Effects.Pipeline {
            val graveyardCreatures = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.Creature
                )
            )
            move(graveyardCreatures, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
        description = "Return all creature cards from your graveyard to the battlefield."
    }

    metadata {
        inBooster = false
        rarity = Rarity.MYTHIC
        collectorNumber = "275"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac79d8f6-7c36-4eb9-8bda-3b813c634399.jpg?1783936437"
    }
}

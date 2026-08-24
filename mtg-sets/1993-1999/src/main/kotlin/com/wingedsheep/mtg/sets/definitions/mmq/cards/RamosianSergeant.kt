package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Ramosian Sergeant
 * {W}
 * Creature — Human Rebel
 * 1 / 1
 */
val RamosianSergeant = card("Ramosian Sergeant") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Rebel"
    oracleText = "{3}, {T}: Search your library for a Rebel permanent card with mana value 2 or less, put it onto the battlefield, then shuffle."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Permanent.withSubtype("Rebel").manaValueAtMost(2),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Don Hazeltine"
        flavorText = "Her commands are part rallying cry, part sermon, and wholly undeniable."
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef2b036d-5721-4a6e-bf43-69148b90da10.jpg"
    }
}

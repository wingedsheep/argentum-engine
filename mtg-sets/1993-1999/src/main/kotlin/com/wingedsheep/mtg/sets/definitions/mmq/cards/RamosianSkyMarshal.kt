package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Ramosian Sky Marshal
 * {3}{W}{W}
 * Creature — Human Rebel
 * 3 / 3
 */
val RamosianSkyMarshal = card("Ramosian Sky Marshal") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Rebel"
    oracleText = "Flying\n" +
        "{7}, {T}: Search your library for a Rebel permanent card with mana value 6 or less, put it onto the battlefield, then shuffle."
    power = 3
    toughness = 3
    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{7}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Permanent.withSubtype("Rebel").manaValueAtMost(6),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "40"
        artist = "Matt Cavotta"
        flavorText = "The Cho-Arrim fell from the sky onto Mercadia City like a vengeful rain."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16638976-8a78-4233-8ebc-42ea9bb49e0a.jpg"
    }
}

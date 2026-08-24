package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Cateran Brute
 * {2}{B}
 * Creature — Horror Mercenary
 * 2 / 2
 */
val CateranBrute = card("Cateran Brute") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror Mercenary"
    oracleText = "{2}, {T}: Search your library for a Mercenary permanent card with mana value 2 or less, put it onto the battlefield, then shuffle."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Permanent.withSubtype("Mercenary").manaValueAtMost(2),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Edward P. Beard, Jr."
        flavorText = "The Cateran guild prefers to call them \"public relations.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73b6ce76-0ed0-4994-ae2c-d8e51ae09920.jpg"
    }
}

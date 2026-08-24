package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Cateran Persuader
 * {B}{B}
 * Creature — Human Mercenary
 * 2 / 1
 */
val CateranPersuader = card("Cateran Persuader") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Mercenary"
    oracleText = "{1}, {T}: Search your library for a Mercenary permanent card with mana value 1 or less, put it onto the battlefield, then shuffle."
    power = 2
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Permanent.withSubtype("Mercenary").manaValueAtMost(1),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a98bdbf1-32a6-4d9b-8e57-5d3aca6b05bc.jpg"
    }
}

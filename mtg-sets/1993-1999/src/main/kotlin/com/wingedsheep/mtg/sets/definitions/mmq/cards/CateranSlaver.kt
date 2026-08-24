package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Cateran Slaver
 * {4}{B}{B}
 * Creature — Horror Mercenary
 * 5 / 5
 */
val CateranSlaver = card("Cateran Slaver") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror Mercenary"
    oracleText = "Swampwalk (This creature can't be blocked as long as defending player controls a Swamp.)\n" +
        "{5}, {T}: Search your library for a Mercenary permanent card with mana value 5 or less, put it onto the battlefield, then shuffle."
    power = 5
    toughness = 5

    keywords(Keyword.SWAMPWALK)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Permanent.withSubtype("Mercenary").manaValueAtMost(5),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "125"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d293c51-714c-45b8-bfa4-fe35e8f3fbc1.jpg"
    }
}

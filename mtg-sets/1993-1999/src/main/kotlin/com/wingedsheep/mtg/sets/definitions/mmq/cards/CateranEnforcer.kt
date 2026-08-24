package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Cateran Enforcer
 * {3}{B}{B}
 * Creature — Horror Mercenary
 * 4 / 3
 */
val CateranEnforcer = card("Cateran Enforcer") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror Mercenary"
    oracleText = "Fear (This creature can't be blocked except by artifact creatures and/or black creatures.)\n" +
        "{4}, {T}: Search your library for a Mercenary permanent card with mana value 4 or less, put it onto the battlefield, then shuffle."
    power = 4
    toughness = 3

    keywords(Keyword.FEAR)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Permanent.withSubtype("Mercenary").manaValueAtMost(4),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Mike Ploog"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e9b6da8-39da-4fce-89cf-ea972f981331.jpg"
    }
}

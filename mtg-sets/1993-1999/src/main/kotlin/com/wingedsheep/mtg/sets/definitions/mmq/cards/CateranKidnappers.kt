package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Cateran Kidnappers
 * {2}{B}{B}
 * Creature — Human Mercenary
 * 4 / 2
 */
val CateranKidnappers = card("Cateran Kidnappers") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Mercenary"
    oracleText = "{3}, {T}: Search your library for a Mercenary permanent card with mana value 3 or less, put it onto the battlefield, then shuffle."
    power = 4
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Permanent.withSubtype("Mercenary").manaValueAtMost(3),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Carl Critchlow"
        flavorText = "Recruits aren't always volunteers."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3768bdc1-4055-423a-a1cc-69b4c620e3e6.jpg"
    }
}

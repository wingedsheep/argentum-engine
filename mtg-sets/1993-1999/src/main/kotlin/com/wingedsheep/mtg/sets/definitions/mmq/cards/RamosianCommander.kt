package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Ramosian Commander
 * {2}{W}{W}
 * Creature — Human Rebel
 * 2 / 4
 */
val RamosianCommander = card("Ramosian Commander") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Rebel"
    oracleText = "{6}, {T}: Search your library for a Rebel permanent card with mana value 5 or less, put it onto the battlefield, then shuffle."
    power = 2
    toughness = 4

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Permanent.withSubtype("Rebel").manaValueAtMost(5),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "36"
        artist = "Scott Hampton"
        flavorText = "\"Cho-Manno guides your spirit. I guide your sword.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/6/867f5d82-71c2-455f-ab16-5a32bba46986.jpg"
    }
}

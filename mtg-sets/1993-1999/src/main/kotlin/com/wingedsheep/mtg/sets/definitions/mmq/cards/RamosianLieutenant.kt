package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Ramosian Lieutenant
 * {1}{W}
 * Creature — Human Rebel
 * 1 / 2
 */
val RamosianLieutenant = card("Ramosian Lieutenant") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Rebel"
    oracleText = "{4}, {T}: Search your library for a Rebel permanent card with mana value 3 or less, put it onto the battlefield, then shuffle."
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Permanent.withSubtype("Rebel").manaValueAtMost(3),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Alan Pollack"
        flavorText = "\"Give me your hand and I will give you victory.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/e/debe840a-ebc9-43c4-9bf7-7eb292b65bf9.jpg"
    }
}

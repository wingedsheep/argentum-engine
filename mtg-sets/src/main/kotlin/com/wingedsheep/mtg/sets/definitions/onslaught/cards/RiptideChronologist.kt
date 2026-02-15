package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChooseCreatureTypeUntapEffect

/**
 * Riptide Chronologist
 * {3}{U}{U}
 * Creature — Human Wizard
 * 1/3
 * {U}, Sacrifice Riptide Chronologist: Untap all creatures of the creature type of your choice.
 */
val RiptideChronologist = card("Riptide Chronologist") {
    manaCost = "{3}{U}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "{U}, Sacrifice Riptide Chronologist: Untap all creatures of the creature type of your choice."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.SacrificeSelf)
        effect = ChooseCreatureTypeUntapEffect
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Matt Cavotta"
        flavorText = "\"The correct course of history is the one in which we survive.\""
        imageUri = "https://cards.scryfall.io/large/front/a/c/ac3e7bf3-4823-44b5-9555-f9a3024f9929.jpg?1562934549"
    }
}

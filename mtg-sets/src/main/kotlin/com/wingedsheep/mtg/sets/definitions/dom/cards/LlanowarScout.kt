package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Llanowar Scout
 * {1}{G}
 * Creature — Elf Scout
 * 1/3
 * {T}: You may put a land card from your hand onto the battlefield.
 */
val LlanowarScout = card("Llanowar Scout") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    power = 1
    toughness = 3
    oracleText = "{T}: You may put a land card from your hand onto the battlefield."

    activatedAbility {
        cost = Costs.Tap
        effect = HandPatterns.putFromHand(filter = GameObjectFilter.Land)
        manaAbility = false
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "170"
        artist = "Wayne Reynolds"
        flavorText = "\"Llanowar elfhames occupy different heights within the great forest. Elves of the Loridalh and Kelfae hames can see neither earth nor sky.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8026d80-2e91-46f9-ae97-1f137848236c.jpg?1562742734"
    }
}

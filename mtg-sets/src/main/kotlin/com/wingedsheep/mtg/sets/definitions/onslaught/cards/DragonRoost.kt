package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreateTokenEffect

/**
 * Dragon Roost
 * {4}{R}{R}
 * Enchantment
 * {5}{R}{R}: Create a 5/5 red Dragon creature token with flying.
 */
val DragonRoost = card("Dragon Roost") {
    manaCost = "{4}{R}{R}"
    typeLine = "Enchantment"

    activatedAbility {
        cost = Costs.Mana("{5}{R}{R}")
        effect = CreateTokenEffect(
            power = 5,
            toughness = 5,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/large/front/5/7/57b8e825-eef6-4d37-892a-6e28597679ee.jpg?1717193756"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "198"
        artist = "Luca Zontini"
        flavorText = "\"The hellkites are content to eat goblins and leave us alone. It's the goblins I'm worried about.\"\n—Aven soldier"
        imageUri = "https://cards.scryfall.io/large/front/9/5/95e4f28b-c7a7-4450-b477-73e4559f0276.jpg?1562930321"
    }
}

package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreateTokenEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.GroupFilter

/**
 * Mobilization
 * {2}{W}
 * Enchantment
 * Soldier creatures have vigilance.
 * {2}{W}: Create a 1/1 white Soldier creature token.
 */
val Mobilization = card("Mobilization") {
    manaCost = "{2}{W}"
    typeLine = "Enchantment"

    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.VIGILANCE,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Soldier"))
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        effect = CreateTokenEffect(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            imageUri = "https://cards.scryfall.io/large/front/b/1/b159b57d-bc52-4cef-ac7a-e364e40c3d03.jpg?1761614919"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Carl Critchlow"
        flavorText = "\"Newcomers to Otaria find themselves at the bottom of the heap. In the pits, they at least have the chance to climb atop a heap of bodies.\""
        imageUri = "https://cards.scryfall.io/large/front/6/5/653cc07b-0f53-4b5b-9c5f-885b8b4a6e5f.jpg?1562918873"
    }
}

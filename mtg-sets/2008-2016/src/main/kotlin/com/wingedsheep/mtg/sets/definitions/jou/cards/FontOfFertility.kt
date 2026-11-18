package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Font of Fertility
 * {G}
 * Enchantment
 *
 * {1}{G}, Sacrifice this enchantment: Search your library for a basic land card, put it onto the
 * battlefield tapped, then shuffle.
 */
val FontOfFertility = card("Font of Fertility") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "{1}{G}, Sacrifice this enchantment: Search your library for a basic land card, put it " +
        "onto the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.SacrificeSelf)
        effect = Patterns.Library.searchLibrary(
            filter = Filters.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Jack Wang"
        flavorText = "Drink deep, and know the path to the heart of the wilds."
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac8d1ecb-de7c-4641-81da-13117820195c.jpg?1783939415"
    }
}

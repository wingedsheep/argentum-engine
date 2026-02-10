package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreateTokenEffect

/**
 * Centaur Glade
 * {3}{G}{G}
 * Enchantment
 * {2}{G}{G}: Create a 3/3 green Centaur creature token.
 */
val CentaurGlade = card("Centaur Glade") {
    manaCost = "{3}{G}{G}"
    typeLine = "Enchantment"

    activatedAbility {
        cost = Costs.Mana("{2}{G}{G}")
        effect = CreateTokenEffect(
            power = 3,
            toughness = 3,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Centaur"),
            imageUri = "https://cards.scryfall.io/large/front/9/8/985be507-6125-4db2-b99f-8b61149ffeeb.jpg?1562636802"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "251"
        artist = "Alex Horley-Orlandelli"
        flavorText = "The Mirari called to the centaurs, and all who heard it were forever changed."
        imageUri = "https://cards.scryfall.io/large/front/1/c/1c75f9c8-9640-4f64-b32a-916436e461fc.jpg?1562901689"
    }
}

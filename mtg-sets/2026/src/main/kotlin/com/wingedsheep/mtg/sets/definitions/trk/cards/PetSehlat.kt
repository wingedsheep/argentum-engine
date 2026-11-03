package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Pet Sehlat
 * {2}{W}
 * Creature — Bear
 * 4/3
 *
 * This creature can't attack alone.
 */
val PetSehlat = card("Pet Sehlat") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bear"
    power = 4
    toughness = 3
    oracleText = "This creature can't attack alone."

    staticAbility {
        ability = CantAttackUnlessCoAttacker(coAttackerFilter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Narendra Bintara Adi"
        flavorText = "The obedience of a Vulcan sehlat makes it a logical choice for a family pet. " +
            "Some humans, facing six-inch fangs, question that logic."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/477a6a46-7fda-4666-87d9-a35c953dd94d.jpg?1793361200"
    }
}

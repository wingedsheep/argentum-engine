package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.beamMeUp
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Adversary by Algorithm
 * {2}{G}
 * Sorcery
 * Create a 3/3 colorless Illusion artifact creature token.
 * Beam me up {3}{G} (You may cast this card from your graveyard for {3}{G} if you also return a
 * creature you control to its owner's hand. Then exile this spell.)
 */
val AdversaryByAlgorithm = card("Adversary by Algorithm") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create a 3/3 colorless Illusion artifact creature token.\n" +
        "Beam me up {3}{G} (You may cast this card from your graveyard for {3}{G} if you also " +
        "return a creature you control to its owner's hand. Then exile this spell.)"
    beamMeUp("{3}{G}")
    spell {
        effect = Effects.CreateToken(
            power = 3, toughness = 3,
            creatureTypes = setOf("Illusion"),
            artifactToken = true
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Tony Foti"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41644f06-d08f-406c-a56a-47e1b5273982.jpg?1793013240"
    }
}

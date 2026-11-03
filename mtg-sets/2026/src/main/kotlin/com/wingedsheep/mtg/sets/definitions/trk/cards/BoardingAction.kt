package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.beamMeUp
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Boarding Action
 * {1}{R}
 * Sorcery
 * Create a 2/1 red Klingon Warrior creature token with haste.
 * Beam me up {2}{R} (You may cast this card from your graveyard for {2}{R} if you also return a
 * creature you control to its owner's hand. Then exile this spell.)
 */
val BoardingAction = card("Boarding Action") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create a 2/1 red Klingon Warrior creature token with haste.\n" +
        "Beam me up {2}{R} (You may cast this card from your graveyard for {2}{R} if you also " +
        "return a creature you control to its owner's hand. Then exile this spell.)"
    beamMeUp("{2}{R}")
    spell {
        effect = Effects.CreateToken(
            power = 2, toughness = 1, colors = setOf(Color.RED),
            creatureTypes = setOf("Klingon", "Warrior"), keywords = setOf(Keyword.HASTE)
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a0d33cd-f81c-4950-81ed-9536061eca8a.jpg?1793361310"
    }
}

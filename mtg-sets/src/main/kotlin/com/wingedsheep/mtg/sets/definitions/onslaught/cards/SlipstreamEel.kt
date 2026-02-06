package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessDefenderControlsLandType
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Slipstream Eel
 * {5}{U}{U}
 * Creature — Fish Beast
 * 6/6
 * Slipstream Eel can't attack unless defending player controls an Island.
 * Cycling {1}{U}
 */
val SlipstreamEel = card("Slipstream Eel") {
    manaCost = "{5}{U}{U}"
    typeLine = "Creature — Fish Beast"
    power = 6
    toughness = 6

    staticAbility {
        ability = CantAttackUnlessDefenderControlsLandType("Island")
    }

    keywordAbility(KeywordAbility.cycling("{1}{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Mark Tedin"
        flavorText = "It's a fine way to travel, if you don't mind the smell."
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9d06a1f-00b7-440d-849d-efc466d73f29.jpg?1562950698"
    }
}

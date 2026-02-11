package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Riptide Biologist
 * {1}{U}
 * Creature — Human Wizard
 * 1/2
 * Protection from Beasts
 * Morph {2}{U}
 */
val RiptideBiologist = card("Riptide Biologist") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 2

    keywordAbility(KeywordAbility.ProtectionFromCreatureSubtype("Beast"))
    morph = "{2}{U}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Justin Sweet"
        flavorText = "\"I gave it two choices: life in the lab or death in the hunt.\""
        imageUri = "https://cards.scryfall.io/large/front/4/d/4d399b71-c365-492c-976e-2c79d97d08bc.jpg?1562913054"
    }
}

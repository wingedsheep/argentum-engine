package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Drakewing Krasis
 * {1}{G}{U}
 * Creature — Lizard Drake
 * 3/1
 * Flying, trample
 */
val DrakewingKrasis = card("Drakewing Krasis") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Creature — Lizard Drake"
    oracleText = "Flying, trample"
    power = 3
    toughness = 1

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Johann Bodin"
        flavorText = "Cautious not to repeat Vig's mistakes, the Simic fuse life essences using magic alone. Each result is called a krasis."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/016d1d17-ba5c-4168-9a3d-232bdcc98c80.jpg"
    }
}

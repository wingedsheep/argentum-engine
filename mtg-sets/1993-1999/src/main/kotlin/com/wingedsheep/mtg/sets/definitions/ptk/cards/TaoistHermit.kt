package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Taoist Hermit
 * {2}{G}
 * Creature — Human Mystic
 * 2/2
 * Hexproof (This creature can't be the target of spells or abilities your opponents control.)
 */
val TaoistHermit = card("Taoist Hermit") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Mystic"
    power = 2
    toughness = 2
    oracleText = "Hexproof (This creature can't be the target of spells or abilities your opponents control.)"

    keywords(Keyword.HEXPROOF)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Wang Yuqun"
        flavorText = "Taoists chose to be hermits for many reasons, but all shared one unchanging goal: to follow the Tao."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d818c231-8d66-4024-91de-fe29f8622902.jpg"
    }
}

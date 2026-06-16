package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rohirrim Lancer
 * {R}
 * Creature — Human Knight
 * 1/1
 *
 * Menace
 * When this creature dies, the Ring tempts you.
 */
val RohirrimLancer = card("Rohirrim Lancer") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Knight"
    power = 1
    toughness = 1
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\nWhen this creature dies, the Ring tempts you."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.TheRingTemptsYou()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Anastasia Balakchina"
        flavorText = "\"Dire deeds awake, dark is it eastward. Let horse be bridled, horn be sounded!\"\n—Théoden"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f506f9f-4c0d-44e8-9f81-8403d808d0e4.jpg?1686969151"
    }
}

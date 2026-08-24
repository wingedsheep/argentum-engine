package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spring of Eternal Peace
 * {3}{G}{G}
 * Sorcery
 *
 * You gain 8 life.
 */
val SpringOfEternalPeace = card("Spring of Eternal Peace") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "You gain 8 life."

    spell {
        effect = Effects.GainLife(8)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Wang Yuqun"
        flavorText = "Bathing in this spring could cure ailments of body and mind alike."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6862d7a-04ee-48ac-a5b3-46a4e8694d5b.jpg"
    }
}

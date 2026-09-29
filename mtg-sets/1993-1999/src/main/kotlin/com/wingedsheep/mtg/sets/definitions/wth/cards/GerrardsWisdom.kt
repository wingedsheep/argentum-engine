package com.wingedsheep.mtg.sets.definitions.wth.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity

val GerrardsWisdom = card("Gerrard's Wisdom") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "You gain 2 life for each card in your hand."

    spell {
        effect = Effects.GainLife(DynamicAmounts.cardsInYourHand() * 2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Heather Hudson"
        flavorText = "\"Fighting without an army is called a duel, and you'll lose a duel if your enemy comes expecting a war.\"\n—Gerrard of the *Weatherlight*"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f81defa5-edb4-4f1f-b13c-7cfb34511138.jpg?1783946748"
    }
}

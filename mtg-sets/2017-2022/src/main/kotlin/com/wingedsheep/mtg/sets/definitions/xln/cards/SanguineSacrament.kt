package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.times

/**
 * Sanguine Sacrament
 * {X}{W}{W}
 * Instant
 *
 * You gain twice X life. Put Sanguine Sacrament on the bottom of its owner's library.
 */
val SanguineSacrament = card("Sanguine Sacrament") {
    manaCost = "{X}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "You gain twice X life. Put Sanguine Sacrament on the bottom of its owner's library."

    spell {
        effect = Effects.GainLife(DynamicAmounts.xValue() * 2)
        selfPutOnBottomOfLibrary()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Bastien L. Deharme"
        flavorText = "\"As the sun sets to make way for dawn, so we turn to darkness to bring about the " +
            "coming salvation.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9fae153-d86a-42b5-9c1c-6dbd56118ecb.jpg"
    }
}

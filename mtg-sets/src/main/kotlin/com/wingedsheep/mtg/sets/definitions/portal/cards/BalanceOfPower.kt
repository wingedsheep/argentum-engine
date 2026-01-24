package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.targeting.TargetOpponent

/**
 * Balance of Power
 * {3}{U}{U}
 * Sorcery
 * If target opponent has more cards in hand than you, draw cards equal to the difference.
 */
val BalanceOfPower = card("Balance of Power") {
    manaCost = "{3}{U}{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = DrawCardsEffect(DynamicAmount.HandSizeDifferenceFromTargetOpponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/3875e753-117a-40b6-9cd2-dd5dfd11a38d.jpg"
    }
}

package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Festive Funeral
 * {4}{B}
 * Instant
 * Target creature gets -X/-X until end of turn, where X is the number of cards in your graveyard.
 *
 * X is locked in as the spell resolves, while Festive Funeral is still on the stack (rulings).
 */
val FestiveFuneral = card("Festive Funeral") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -X/-X until end of turn, where X is the number of cards in your graveyard."

    spell {
        val creature = target(TargetFilter.Creature)
        val x = DynamicAmounts.cardsInYourGraveyard()
        effect = Effects.ModifyStats(-x, -x, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Zack Stella"
        flavorText = "\"Even as we mourn the last, we look to the next with joy. Hopefully, my new suitor will bring the Cauldron home.\"\n—Ayara, First of Locthwain"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90a90af0-eb40-40da-bf4b-f0af687c6430.jpg?1783932641"
        ruling("2019-10-04", "The value of X is determined only as Festive Funeral begins to resolve. The target creature's power and toughness won't change if other cards are put into your graveyard later in the turn.")
        ruling("2019-10-04", "Festive Funeral is still on the stack while you determine the value of X. It doesn't contribute to the value of X.")
    }
}

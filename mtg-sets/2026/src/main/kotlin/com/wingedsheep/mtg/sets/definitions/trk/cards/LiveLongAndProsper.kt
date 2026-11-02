package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Live Long and Prosper
 * {5}{W}{U}
 * Sorcery
 *
 * This spell can't be countered.
 * Double your life total. Draw cards equal to the number of cards in your hand. You have no
 * maximum hand size until your next turn.
 *
 * Doubling is "your life total becomes twice what it was" (Revival // Revenge's shape). The hand is
 * counted on resolution, after the life change and while this spell is still on the stack.
 */
val LiveLongAndProsper = card("Live Long and Prosper") {
    manaCost = "{5}{W}{U}"
    typeLine = "Sorcery"
    colorIdentity = "WU"
    oracleText = "This spell can't be countered.\nDouble your life total. Draw cards equal to the number of cards in your hand. " +
        "You have no maximum hand size until your next turn."
    cantBeCountered = true

    spell {
        effect = Effects.SetLifeTotal(DynamicAmounts.lifeTotal(Player.You) * 2) then
            Effects.DrawCards(DynamicAmounts.cardsInYourHand()) then
            Effects.RemoveMaximumHandSize(duration = Duration.UntilYourNextTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Stella Spente"
        flavorText = "\"Logic is the beginning of wisdom, not the end.\"\n—Mr. Spock"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fc50ffc-3b4d-4fb3-898f-c308a63eb6e9.jpg?1792508356"
    }
}

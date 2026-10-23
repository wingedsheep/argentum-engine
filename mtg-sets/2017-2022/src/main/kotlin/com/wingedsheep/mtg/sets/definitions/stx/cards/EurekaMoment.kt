package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Eureka Moment — Strixhaven: School of Mages #184 (canonical printing)
 * {2}{G}{U} · Instant
 *
 * Draw two cards. You may put a land card from your hand onto the battlefield.
 *
 * Growth Spiral with a second draw. [Patterns.Hand.putFromHand] selects *up to* one land, which is
 * the "you may"; it is not playing a land, so it ignores land drops and timing (ruling).
 */
val EurekaMoment = card("Eureka Moment") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Draw two cards. You may put a land card from your hand onto the battlefield."

    spell {
        effect = Effects.DrawCards(2) then
            Patterns.Hand.putFromHand(filter = GameObjectFilter.Land)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Anna Steinbauer"
        flavorText = "\"At first, the equations were simply numbers. But in those numbers I saw a story, and in that story I saw a pattern, and suddenly it all fit together!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e400c9b7-c789-49dd-9f72-b9d1df03fcca.jpg?1783927314"
        ruling("2021-04-16", "Putting a land card from your hand onto the battlefield is not the same as playing a land. You may do it even if you have already played a land on your turn, or if you're casting Eureka Moment on your opponent's turn.")
    }
}

package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tragic Lesson
 * {2}{U}
 * Instant
 * Draw two cards. Then discard a card unless you return a land you control to its owner's hand.
 *
 * The punisher choice is made at resolution, after the draw (2017-07-14 ruling), so it composes as
 * `DrawCards(2) then PayOrSuffer(return a land you control, discard a card)`.
 */
val TragicLesson = card("Tragic Lesson") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw two cards. Then discard a card unless you return a land you control to its owner's hand."
    spell {
        effect = Effects.DrawCards(2) then
            Effects.PayOrSuffer(
                cost = Costs.pay.ReturnToHand(filter = GameObjectFilter.Land),
                suffer = Effects.Discard(1)
            )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Joseph Meehan"
        flavorText = "Though Kefnet's followers feverishly searched his last words for some final riddle, they found only the gurgles of a dying god."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0f0353c-f1e0-49db-9edc-eea9090de872.jpg?1783936046"
        ruling(
            "2017-07-14",
            "You don't choose which land to return to its owner's hand or whether to discard a card " +
                "instead until you see the two cards you draw."
        )
    }
}

package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Test of Identity
 * {1}{R}
 * Instant
 *
 * Choose one —
 * • I Am Klingon — Target Klingon gets +1/+1 and gains double strike until end of turn.
 * • I Am Starfleet — Discard a card. If you do, draw two cards.
 *
 * "Target Klingon" is any Klingon permanent, not just a creature. "If you do" is modeled with
 * [Effects.IfYouDo]: the draw only happens when a card was actually discarded.
 */
val TestOfIdentity = card("Test of Identity") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• I Am Klingon — Target Klingon gets +1/+1 and gains double strike until end of turn.\n" +
        "• I Am Starfleet — Discard a card. If you do, draw two cards."

    spell {
        modal(chooseCount = 1) {
            mode("I Am Klingon — Target Klingon gets +1/+1 and gains double strike until end of turn") {
                val klingon = target(TargetFilter(GameObjectFilter.Permanent.withSubtype("Klingon")))
                effect = Effects.ModifyStats(1, 1, klingon) then
                    Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, klingon)
            }

            mode("I Am Starfleet — Discard a card. If you do, draw two cards") {
                effect = Effects.IfYouDo(
                    action = Patterns.Hand.discardCards(1),
                    then = Effects.DrawCards(2),
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"I am Worf, son of Mogh, House of Martok, son of Sergey, House of Rozhenko.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e1c0423-bf58-47a1-a838-c15bbf40eaba.jpg?1793359534"
    }
}

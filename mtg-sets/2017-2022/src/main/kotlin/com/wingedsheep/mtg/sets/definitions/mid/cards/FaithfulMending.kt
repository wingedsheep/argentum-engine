package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Faithful Mending
 * {W}{U}
 * Instant
 * You gain 2 life, draw two cards, then discard two cards.
 * Flashback {1}{W}{U}
 */
val FaithfulMending = card("Faithful Mending") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Instant"
    oracleText = "You gain 2 life, draw two cards, then discard two cards.\n" +
        "Flashback {1}{W}{U} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Effects.GainLife(2) then Effects.DrawCards(2) then Patterns.Hand.discardCards(2)
    }

    keywordAbility(KeywordAbility.flashback("{1}{W}{U}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Caroline Gariba"
        flavorText = "\"What has been looted can be restored. The church lives on within us.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50752ca9-ea85-4c4b-9bf5-4f8759a7dcec.jpg?1783925563"
    }
}

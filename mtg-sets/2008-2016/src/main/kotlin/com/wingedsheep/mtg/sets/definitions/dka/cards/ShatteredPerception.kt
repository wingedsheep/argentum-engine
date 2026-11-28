package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Shattered Perception — Dark Ascension #104 (canonical printing)
 * {2}{R} · Sorcery
 *
 * Discard all the cards in your hand, then draw that many cards.
 * Flashback {5}{R}
 *
 * "That many" is the size of the hand actually discarded — [Patterns.Hand.discardedHand]'s count,
 * the same reading Borrowed Knowledge's second mode uses.
 */
val ShatteredPerception = card("Shattered Perception") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Discard all the cards in your hand, then draw that many cards.\n" +
        "Flashback {5}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Patterns.Hand.discardHand() then Effects.DrawCards(Patterns.Hand.discardedHand.count)
    }

    keywordAbility(KeywordAbility.flashback("{5}{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Terese Nielsen"
        flavorText = "\"You must shatter the fetters of the past. Only then can you truly act.\"\n" +
            "—Rem Karolus, Blade of the Inquisitors"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f5223b4-e0d1-4fc6-a363-ebcdfeee56d1.jpg?1783940811"
    }
}

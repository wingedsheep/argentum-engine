package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sphinx of Uthuun
 * {5}{U}{U}
 * Creature — Sphinx
 * 5/6
 * Flying
 * When this creature enters, reveal the top five cards of your library. An opponent separates
 * those cards into two piles. Put one pile into your hand and the other into your graveyard.
 */
val SphinxOfUthuun = card("Sphinx of Uthuun") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    oracleText = "Flying\nWhen this creature enters, reveal the top five cards of your library. An opponent separates those cards into two piles. Put one pile into your hand and the other into your graveyard."
    power = 5
    toughness = 6

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.factOrFiction(count = 5)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "76"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a290648a-63c3-400b-98d3-5a5aa5505027.jpg"
        ruling("2017-11-17", "You (not your opponent) choose which pile to put into your hand and which to put into your graveyard.")
        ruling("2017-11-17", "A pile can have no cards in it. In this case, you'll choose whether to put all the revealed cards into your hand or into your graveyard.")
        ruling("2017-11-17", "In multiplayer games, you choose an opponent to separate the cards when the ability resolves. This doesn't target that opponent. Because the cards are revealed, all players may see the cards and offer opinions.")
    }
}

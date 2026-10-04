package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Ral and the Implicit Maze
 * {3}{R}{R}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — This Saga deals 2 damage to each creature and planeswalker your opponents control.
 * II — You may discard a card. If you do, exile the top two cards of your library. You may play
 *      them until the end of your next turn.
 * III — Create a Spellgorger Weird token. (It's a {2}{R} 2/2 Weird creature with "Whenever you
 *       cast a noncreature spell, put a +1/+1 counter on Spellgorger Weird.")
 *
 * Per the ruling, chapter III's token is a copy of the Oracle card Spellgorger Weird, so it is
 * minted as a predefined token straight from that card's registered definition — mana cost
 * {2}{R} (mana value 3, red) included, which a token keeps only when its creator defines one
 * (CR 202.1b).
 */
val RalAndTheImplicitMaze = card("Ral and the Implicit Maze") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — This Saga deals 2 damage to each creature and planeswalker your opponents control.\n" +
        "II — You may discard a card. If you do, exile the top two cards of your library. " +
        "You may play them until the end of your next turn.\n" +
        "III — Create a Spellgorger Weird token. (It's a {2}{R} 2/2 Weird creature with " +
        "\"Whenever you cast a noncreature spell, put a +1/+1 counter on Spellgorger Weird.\")"

    sagaChapter(1) {
        effect = Patterns.Group.dealDamageToAll(
            2,
            GroupFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls())
        )
    }

    sagaChapter(2) {
        effect = Effects.May(
            Effects.IfYouDo(
                Patterns.Hand.discardCards(1),
                Patterns.Exile.impulse(count = 2, expiry = MayPlayExpiry.UntilEndOfNextTurn)
            )
        )
    }

    sagaChapter(3) {
        effect = Effects.CreatePredefinedToken("Spellgorger Weird")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebadb7dc-69a4-43c9-a2f8-d846b231c71c.jpg?1783911269"
        ruling("2024-06-07", "You pay all costs and follow all normal timing rules for cards played with Ral and the Implicit Maze's second chapter ability. For example, if one of the exiled cards is a land card, you may play it only during your main phase while the stack is empty.")
        ruling("2024-06-07", "Ral and the Implicit Maze's last ability creates a token that's a copy of the card Spellgorger Weird in the Oracle card reference. Official text for Spellgorger Weird can be found using the Gatherer card database at Gatherer.Wizards.com.")
    }
}

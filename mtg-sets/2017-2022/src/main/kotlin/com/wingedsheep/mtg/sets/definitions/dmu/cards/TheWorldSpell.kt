package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.readAhead
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * The World Spell
 * {5}{G}{G}
 * Enchantment — Saga
 *
 * Read ahead (Choose a chapter and start with that many lore counters. Add one after your draw
 * step. Skipped chapters don't trigger. Sacrifice after III.)
 * I, II — Look at the top seven cards of your library. You may reveal a non-Saga permanent card
 *         from among them and put it into your hand. Put the rest on the bottom of your library in
 *         a random order.
 * III — Put up to two non-Saga permanent cards from your hand onto the battlefield.
 */
val TheWorldSpell = card("The World Spell") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "Read ahead (Choose a chapter and start with that many lore counters. Add one " +
        "after your draw step. Skipped chapters don't trigger. Sacrifice after III.)\n" +
        "I, II — Look at the top seven cards of your library. You may reveal a non-Saga permanent " +
        "card from among them and put it into your hand. Put the rest on the bottom of your library " +
        "in a random order.\n" +
        "III — Put up to two non-Saga permanent cards from your hand onto the battlefield."

    readAhead()

    val nonSagaPermanent = GameObjectFilter.Permanent.notSubtype(Subtype.SAGA)

    for (chapter in 1..2) {
        sagaChapter(chapter) {
            effect = Patterns.Library.lookAtTopRevealMatchingToHand(
                count = 7,
                filter = nonSagaPermanent,
                prompt = "You may reveal a non-Saga permanent card and put it into your hand",
            )
        }
    }

    sagaChapter(3) {
        effect = Patterns.Hand.putFromHand(
            filter = nonSagaPermanent,
            count = 2,
            prompt = "Put up to two non-Saga permanent cards from your hand onto the battlefield",
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "189"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3a9112a-6a1d-4d89-9866-189b55cd326d.jpg?1783921290"

        ruling("2022-09-09", "As a Saga with read ahead enters the battlefield, its controller chooses a number from one to that Saga's greatest chapter number. The Saga enters the battlefield with the chosen number of lore counters. Neither choosing the number nor putting the counters on the Saga use the stack, and neither can be responded to.")
    }
}

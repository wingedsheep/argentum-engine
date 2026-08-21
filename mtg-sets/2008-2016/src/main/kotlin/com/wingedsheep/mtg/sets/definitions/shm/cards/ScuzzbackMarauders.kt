package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Scuzzback Marauders
 * {4}{R/G}
 * Creature — Goblin Warrior
 * 5 / 2
 *
 * Trample
 * Persist (When this creature dies, if it had no -1/-1 counters on it, return it to the battlefield
 * under its owner's control with a -1/-1 counter on it.)
 *
 * - Both abilities are plain keywords; trample first, then persist, matching the printed order Assay
 *   reads off the oracle text.
 * - Persist is engine-live: [Keyword.PERSIST] is read by the death-trigger detector, so the keyword
 *   alone carries the whole behaviour. No triggered ability is authored for the reminder text.
 */
val ScuzzbackMarauders = card("Scuzzback Marauders") {
    manaCost = "{4}{R/G}"
    typeLine = "Creature — Goblin Warrior"
    power = 5
    toughness = 2
    oracleText = "Trample\n" +
        "Persist (When this creature dies, if it had no -1/-1 counters on it, return it to the " +
        "battlefield under its owner's control with a -1/-1 counter on it.)"

    keywords(Keyword.TRAMPLE, Keyword.PERSIST)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "216"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6738d2b7-2773-4146-8f95-8d95b8402266.jpg?1783942720"
    }
}

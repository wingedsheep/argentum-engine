package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gravelgill Axeshark
 * {4}{U/B}
 * Creature — Merfolk Soldier
 * 3 / 3
 *
 * Persist (When this creature dies, if it had no -1/-1 counters on it, return it to the battlefield
 * under its owner's control with a -1/-1 counter on it.)
 *
 * - Persist is engine-live: [Keyword.PERSIST] is read by the death-trigger detector, so the keyword
 *   alone carries the whole behaviour. No triggered ability is authored for the reminder text.
 * - `{U/B}` is a plain hybrid pip in `manaCost`; mana value (5) is derived by the parser.
 */
val GravelgillAxeshark = card("Gravelgill Axeshark") {
    manaCost = "{4}{U/B}"
    typeLine = "Creature — Merfolk Soldier"
    power = 3
    toughness = 3
    oracleText = "Persist (When this creature dies, if it had no -1/-1 counters on it, return it " +
        "to the battlefield under its owner's control with a -1/-1 counter on it.)"

    keywords(Keyword.PERSIST)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Dave Kendall"
        flavorText = "\"Their sharp scales would make good armor, if they'd just stay dead.\"\n" +
            "—Taeryn, cinder armorkeep"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5efc174a-f710-4602-aace-c2165473f6c2.jpg?1783942733"
    }
}

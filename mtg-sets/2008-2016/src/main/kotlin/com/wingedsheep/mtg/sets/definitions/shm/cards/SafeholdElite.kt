package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Safehold Elite
 * {1}{G/W}
 * Creature — Elf Scout
 * 2 / 2
 *
 * Persist (When this creature dies, if it had no -1/-1 counters on it, return it to the battlefield
 * under its owner's control with a -1/-1 counter on it.)
 *
 * - Persist is engine-live: [Keyword.PERSIST] is read by the death-trigger detector, so the keyword
 *   alone carries the whole behaviour. No triggered ability is authored for the reminder text.
 */
val SafeholdElite = card("Safehold Elite") {
    manaCost = "{1}{G/W}"
    typeLine = "Creature — Elf Scout"
    power = 2
    toughness = 2
    oracleText = "Persist (When this creature dies, if it had no -1/-1 counters on it, return it " +
        "to the battlefield under its owner's control with a -1/-1 counter on it.)"

    keywords(Keyword.PERSIST)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "239"
        artist = "Richard Whitters"
        flavorText = "\"I refuse to die—not at the hands of one such as you.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14dee5be-94fd-4b5d-9265-edd1b58c8026.jpg?1783942714"
    }
}

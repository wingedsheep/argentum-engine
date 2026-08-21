package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Boggart Ram-Gang
 * {R/G}{R/G}{R/G}
 * Creature — Goblin Warrior
 * 3 / 3
 *
 * Haste
 * Wither (This deals damage to creatures in the form of -1/-1 counters.)
 *
 * - Keyword-only creature: both [Keyword.HASTE] and [Keyword.WITHER] are engine-live, so the
 *   reminder text needs no separate replacement effect.
 */
val BoggartRamGang = card("Boggart Ram-Gang") {
    manaCost = "{R/G}{R/G}{R/G}"
    typeLine = "Creature — Goblin Warrior"
    power = 3
    toughness = 3
    oracleText = "Haste\n" +
        "Wither (This deals damage to creatures in the form of -1/-1 counters.)"

    keywords(Keyword.HASTE, Keyword.WITHER)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Dave Allsop"
        flavorText = "\"We're going to need a bigger gate.\"\n" +
            "—Bowen, Barrenton guardcaptain"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63e25b70-fa21-4663-aabd-34b7e0b75c34.jpg?1783942723"
    }
}

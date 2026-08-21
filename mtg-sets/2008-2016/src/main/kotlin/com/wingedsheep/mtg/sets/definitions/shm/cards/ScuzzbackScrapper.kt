package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Scuzzback Scrapper
 * {R/G}
 * Creature — Goblin Warrior
 * 1 / 1
 *
 * Wither (This deals damage to creatures in the form of -1/-1 counters.)
 *
 * - Keyword-only creature: [Keyword.WITHER] is engine-live, so the reminder text needs no separate
 *   replacement effect.
 */
val ScuzzbackScrapper = card("Scuzzback Scrapper") {
    manaCost = "{R/G}"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 1
    oracleText = "Wither (This deals damage to creatures in the form of -1/-1 counters.)"

    keywords(Keyword.WITHER)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "217"
        artist = "Scott Altmann"
        flavorText = "The Scuzzback gang scavenges rusty armor covered in barbed protrusions. No threat is more effective than the threat of infection."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e2ed66f-75bd-43d8-90c4-cb0a5827b2f0.jpg?1783942719"
    }
}

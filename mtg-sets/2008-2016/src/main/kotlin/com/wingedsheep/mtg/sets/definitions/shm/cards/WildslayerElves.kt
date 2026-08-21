package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wildslayer Elves
 * {3}{G}
 * Creature — Elf Warrior
 * 3 / 3
 *
 * Wither (This deals damage to creatures in the form of -1/-1 counters.)
 *
 * - Keyword-only creature: [Keyword.WITHER] is engine-live, so the reminder text needs no separate
 *   replacement effect.
 */
val WildslayerElves = card("Wildslayer Elves") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Elf Warrior"
    power = 3
    toughness = 3
    oracleText = "Wither (This deals damage to creatures in the form of -1/-1 counters.)"

    keywords(Keyword.WITHER)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Dave Kendall"
        flavorText = "Some elves battled too long in the deep shadow, their swords dipped too often in tainted flesh and poisoned blood."
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3323467c-132f-469a-90fc-9d3b0fb004aa.jpg?1783942739"
    }
}

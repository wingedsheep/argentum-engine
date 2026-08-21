package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sickle Ripper
 * {1}{B}
 * Creature — Elemental Warrior
 * 2 / 1
 *
 * Wither (This deals damage to creatures in the form of -1/-1 counters.)
 *
 * - Keyword-only creature: [Keyword.WITHER] is engine-live, so the reminder text needs no separate
 *   replacement effect.
 */
val SickleRipper = card("Sickle Ripper") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Elemental Warrior"
    power = 2
    toughness = 1
    oracleText = "Wither (This deals damage to creatures in the form of -1/-1 counters.)"

    keywords(Keyword.WITHER)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Dan Murayama Scott"
        flavorText = "His sickle was forged in the heat of another cinder's funeral pyre."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e07c4e5f-5de7-45ec-b502-0e6d7c5c359d.jpg?1783942752"
    }
}

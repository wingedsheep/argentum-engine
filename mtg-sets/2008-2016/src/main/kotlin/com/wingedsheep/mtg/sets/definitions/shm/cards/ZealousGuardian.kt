package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zealous Guardian
 * {W/U}
 * Creature — Kithkin Soldier
 * 1 / 1
 *
 * Flash
 *
 * - Keyword-only creature: [Keyword.FLASH] is engine-live and carries the casting-timing
 *   permission on its own, so no scripted ability is needed.
 */
val ZealousGuardian = card("Zealous Guardian") {
    manaCost = "{W/U}"
    typeLine = "Creature — Kithkin Soldier"
    power = 1
    toughness = 1
    oracleText = "Flash"

    keywords(Keyword.FLASH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Steven Belledin"
        flavorText = "Parapet watchers patrol the outer edges of the doun, signaling to others who wait patiently in shadow."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9951214b-43a9-4c51-bbb4-05bac81b9866.jpg?1783942733"
    }
}

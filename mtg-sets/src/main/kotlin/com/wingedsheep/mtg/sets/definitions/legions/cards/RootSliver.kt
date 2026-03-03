package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantCantBeCountered

/**
 * Root Sliver
 * {3}{G}
 * Creature — Sliver
 * 2/2
 * This spell can't be countered.
 * Sliver spells can't be countered.
 */
val RootSliver = card("Root Sliver") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Sliver"
    power = 2
    toughness = 2
    oracleText = "This spell can't be countered.\nSliver spells can't be countered."

    cantBeCountered = true

    staticAbility {
        ability = GrantCantBeCountered(
            filter = GameObjectFilter.Any.withSubtype("Sliver")
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Matt Thompson"
        flavorText = "\"It would take another apocalypse to stop the slivers now.\" —Riptide Project researcher"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fdf5a106-5fb7-40e4-82a7-db559302a923.jpg?1562946276"
    }
}

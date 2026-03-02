package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType

/**
 * Quick Sliver
 * {1}{G}
 * Creature — Sliver
 * 1/1
 * Flash
 * Any player may cast Sliver spells as though they had flash.
 */
val QuickSliver = card("Quick Sliver") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Sliver"
    power = 1
    toughness = 1
    oracleText = "Flash\nAny player may cast Sliver spells as though they had flash."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Any.withSubtype("Sliver")
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "John Avon"
        flavorText = "The directors of the Riptide Project wanted instant results on the sliver experiments. They got their wish."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30a60b2d-aeeb-4dbf-bf1a-20a274fe323f.jpg?1562904913"
    }
}

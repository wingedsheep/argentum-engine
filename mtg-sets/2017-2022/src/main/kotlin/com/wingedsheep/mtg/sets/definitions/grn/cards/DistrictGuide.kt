package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * District Guide — Guilds of Ravnica #128
 * {2}{G} · Creature — Elf Scout · 2/2
 *
 * When this creature enters, you may search your library for a basic land card or Gate card,
 * reveal it, put it into your hand, then shuffle.
 */
val DistrictGuide = card("District Guide") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you may search your library for a basic land card or Gate card, " +
        "reveal it, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand or GameObjectFilter.Land.withSubtype("Gate"),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Tianhua X"
        flavorText = "\"The route is direct but challenging. Incidentally, how are you with heights?\""
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c150deec-3287-40ef-9b9c-c22bf5d70b02.jpg?1783934152"
    }
}

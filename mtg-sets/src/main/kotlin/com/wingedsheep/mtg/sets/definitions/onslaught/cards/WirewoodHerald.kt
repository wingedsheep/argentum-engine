package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.scripting.SearchLibraryEffect

/**
 * Wirewood Herald
 * {1}{G}
 * Creature — Elf
 * 1/1
 * When Wirewood Herald dies, you may search your library for an Elf card,
 * reveal that card, put it into your hand, then shuffle.
 */
val WirewoodHerald = card("Wirewood Herald") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Elf"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.Dies
        effect = MayEffect(
            SearchLibraryEffect(
                unifiedFilter = GameObjectFilter.Any.withSubtype("Elf"),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "302"
        artist = "Alex Horley-Orlandelli"
        flavorText = "The goblins laughed as the elf ran away, until more came back."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35724e9f-efa6-47e7-ab4d-7defe38ba576.jpg?1562907555"
    }
}

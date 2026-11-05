package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Hour of Promise
 * {4}{G}
 * Sorcery
 * Search your library for up to two land cards, put them onto the battlefield tapped, then shuffle.
 * Then if you control three or more Deserts, create two 2/2 black Zombie creature tokens.
 *
 * The Desert count is an [Effects.If] sequenced after the search, so it is evaluated on resolution
 * once the found lands are on the battlefield — a Desert fetched by this spell counts (2017-07-14
 * ruling).
 */
val HourOfPromise = card("Hour of Promise") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to two land cards, put them onto the battlefield tapped, then shuffle. " +
        "Then if you control three or more Deserts, create two 2/2 black Zombie creature tokens."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land,
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        ) then Effects.If(
            condition = Conditions.YouControlAtLeast(3, GameObjectFilter.Permanent.withSubtype("Desert")),
            then = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"),
                count = 2,
                imageUri = "https://cards.scryfall.io/normal/front/b/5/b5bd6905-79be-4d2c-a343-f6e6a181b3e6.jpg?1783936411"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Jonas De Ro"
        flavorText = "With the Hekma breached and its protection removed, the deserts swept in."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5ee374f-31dc-4b2c-beba-431d5b1126d0.jpg?1783936018"
        ruling("2017-07-14", "When Hour of Promise checks the number of Deserts you control, it includes any Deserts you found earlier with Hour of Promise.")
    }
}

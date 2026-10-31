package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shared Summons — Core Set 2020 #193
 * {3}{G}{G} · Instant
 *
 * Search your library for up to two creature cards with different names, reveal them, put them
 * into your hand, then shuffle.
 *
 * Three Dreams' pipeline over creature cards: gather the library's creature cards → `chooseUpTo(2)`
 * under [SelectionRestriction.OnePerCardName] → to hand revealed → shuffle.
 */
val SharedSummons = card("Shared Summons") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Search your library for up to two creature cards with different names, reveal them, " +
        "put them into your hand, then shuffle."

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Creature),
                search = true
            )
            val found = chooseUpTo(
                2,
                from = searchable,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Search for up to two creature cards with different names"
            )
            toHand(found, revealed = true)
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Aaron Miller"
        flavorText = "\"In times of need, the forest creates its own protectors.\"\n—Vivien Reid"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e1e49b6-ed0f-42f5-af22-6f4aa23ce4ef.jpg?1783932958"
    }
}

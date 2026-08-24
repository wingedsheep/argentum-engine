package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Imperial Seal
 * {B}
 * Sorcery
 *
 * Search your library for a card, then shuffle and put that card on top. You lose 2 life.
 *
 * The search half is the shared `Patterns.Library.searchLibrary` pipeline with
 * [SearchDestination.TOP_OF_LIBRARY], which shuffles before placing the found card on top.
 */
val ImperialSeal = card("Imperial Seal") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Search your library for a card, then shuffle and put that card on top. You lose 2 life."

    spell {
        effect = Effects.Composite(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any,
                destination = SearchDestination.TOP_OF_LIBRARY,
            ),
            Effects.LoseLife(2, EffectTarget.Controller),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Li Tie"
        flavorText = "\"If Heaven has placed it in your hands, it means that the throne is destined to be yours.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/2/822e30db-40c5-4099-868b-185ad9b7c7dc.jpg"
    }
}

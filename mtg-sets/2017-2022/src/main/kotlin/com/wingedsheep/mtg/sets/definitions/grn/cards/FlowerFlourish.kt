package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Flower // Flourish (GRN 226) — split-layout spell (CR 709).
 *
 * Flower {G/W} — Sorcery
 *   Search your library for a basic Forest or Plains card, reveal it, put it into your hand, then shuffle.
 *
 * Flourish {4}{G}{W} — Sorcery
 *   Creatures you control get +2/+2 until end of turn.
 */
val FlowerFlourish = card("Flower // Flourish") {
    layout = CardLayout.SPLIT
    colorIdentity = "GW"

    face("Flower") {
        manaCost = "{G/W}"
        typeLine = "Sorcery"
        oracleText = "Search your library for a basic Forest or Plains card, reveal it, put it into your hand, then shuffle."

        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand.withAnySubtype("Forest", "Plains"),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true
            )
        }
    }

    face("Flourish") {
        manaCost = "{4}{G}{W}"
        typeLine = "Sorcery"
        oracleText = "Creatures you control get +2/+2 until end of turn."

        spell {
            effect = Patterns.Group.modifyStatsForAll(2, 2, Filters.Group.creaturesYouControl)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/feb4b39f-d309-49ba-b427-240b7fdc1099.jpg?1783934111"
    }
}

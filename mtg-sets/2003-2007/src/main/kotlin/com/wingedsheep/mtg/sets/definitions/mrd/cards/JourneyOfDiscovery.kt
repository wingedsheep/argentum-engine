package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.entwineModal
import com.wingedsheep.sdk.dsl.entwine
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Journey of Discovery
 * {2}{G}
 * Sorcery
 * Choose one —
 * • Search your library for up to two basic land cards, reveal them, put them into your hand, then shuffle.
 * • You may play up to two additional lands this turn.
 * Entwine {2}{G} (Choose both if you pay the entwine cost.)
 */
val JourneyOfDiscovery = card("Journey of Discovery") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Search your library for up to two basic land cards, reveal them, put them into your hand, then shuffle.\n" +
        "• You may play up to two additional lands this turn.\n" +
        "Entwine {2}{G} (Choose both if you pay the entwine cost.)"

    entwine("{2}{G}")

    spell {
        entwineModal {
            mode("Search your library for up to two basic land cards") {
                effect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand,
                    count = 2,
                    destination = SearchDestination.HAND,
                    shuffleAfter = true,
                    reveal = true
                )
            }
            mode(
                "You may play up to two additional lands this turn",
                Effects.PlayAdditionalLands(count = 2)
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "John Matson"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3f4356f-8cfb-43ed-bdf9-6191bb563388.jpg?1783944533"
        ruling(
            "2004-12-01",
            "If you cast an entwined Journey of Discovery during an opponent’s turn, you can’t " +
                "play two lands during that turn."
        )
    }
}

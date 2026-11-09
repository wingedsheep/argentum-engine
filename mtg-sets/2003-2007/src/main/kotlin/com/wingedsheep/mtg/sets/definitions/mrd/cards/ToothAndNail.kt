package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.entwineModal
import com.wingedsheep.sdk.dsl.entwine
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tooth and Nail
 * {5}{G}{G}
 * Sorcery
 * Choose one —
 * • Search your library for up to two creature cards, reveal them, put them into your hand, then shuffle.
 * • Put up to two creature cards from your hand onto the battlefield.
 * Entwine {2} (Choose both if you pay the entwine cost.)
 */
val ToothAndNail = card("Tooth and Nail") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Search your library for up to two creature cards, reveal them, put them into your hand, then shuffle.\n" +
        "• Put up to two creature cards from your hand onto the battlefield.\n" +
        "Entwine {2} (Choose both if you pay the entwine cost.)"

    entwine("{2}")

    spell {
        entwineModal {
            mode("Search your library for up to two creature cards") {
                effect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.Creature,
                    count = 2,
                    destination = SearchDestination.HAND,
                    shuffleAfter = true,
                    reveal = true,
                )
            }
            mode("Put up to two creature cards from your hand onto the battlefield") {
                effect = Effects.Pipeline {
                    val candidates = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature))
                    val chosen = chooseUpTo(
                        2,
                        from = candidates,
                        prompt = "Choose up to two creature cards to put onto the battlefield"
                    )
                    move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
                }
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "134"
        artist = "Greg Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02f0067c-2d38-46bd-b52e-070c2ce424f0.jpg?1783944531"
    }
}

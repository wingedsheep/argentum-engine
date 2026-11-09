package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.entwineModal
import com.wingedsheep.sdk.dsl.entwine
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Temporal Cascade
 * {5}{U}{U}
 * Sorcery
 * Choose one —
 * • Each player shuffles their hand and graveyard into their library.
 * • Each player draws seven cards.
 * Entwine {2} (Choose both if you pay the entwine cost.)
 */
val TemporalCascade = card("Temporal Cascade") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Each player shuffles their hand and graveyard into their library.\n" +
        "• Each player draws seven cards.\n" +
        "Entwine {2} (Choose both if you pay the entwine cost.)"

    entwine("{2}")

    spell {
        entwineModal {
            mode("Each player shuffles their hand and graveyard into their library") {
                effect = Effects.ForEachPlayer(
                    players = Player.Each,
                    Effects.Pipeline {
                        val temporalCascadeCards = gather(
                            CardSource.FromMultipleZones(
                                zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                                player = Player.You,
                            )
                        )
                        move(
                            temporalCascadeCards,
                            CardDestination.ToZone(
                                Zone.LIBRARY,
                                Player.You,
                                ZonePlacement.Shuffled,
                            )
                        )
                    },
                )
            }
            mode(
                "Each player draws seven cards",
                Effects.ForEachPlayer(Player.Each, Effects.DrawCards(7)),
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Puddnhead"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daf180c6-7ab6-4922-9f5e-73b4f2c9488a.jpg?1783944551"
        ruling(
            "2013-07-01",
            "This card won’t be put into your graveyard until after it’s finished resolving, which means " +
                "it won’t be shuffled into your library as part of its own effect.",
        )
    }
}

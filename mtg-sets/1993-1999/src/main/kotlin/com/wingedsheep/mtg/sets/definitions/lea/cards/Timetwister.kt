package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Timetwister
 * {2}{U}
 * Sorcery
 * Each player shuffles their hand and graveyard into their library, then draws seven cards.
 * (Then put Timetwister into its owner's graveyard.)
 *
 * Two [Effects.ForEachPlayer] passes, matching the oracle's "each player shuffles …, then draws":
 * every player shuffles their hand and graveyard (one gather across both zones, one shuffled move —
 * the same body as Temporal Cascade's first mode) before anyone draws. Timetwister is still on the
 * stack while it resolves, so it is never part of its own shuffle.
 */
val Timetwister = card("Timetwister") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Each player shuffles their hand and graveyard into their library, then draws seven cards. " +
        "(Then put Timetwister into its owner's graveyard.)"

    spell {
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            Effects.Pipeline {
                val handAndGraveyard = gather(
                    CardSource.FromMultipleZones(
                        zones = listOf(Zone.HAND, Zone.GRAVEYARD),
                        player = Player.You,
                    )
                )
                move(
                    handAndGraveyard,
                    CardDestination.ToZone(
                        Zone.LIBRARY,
                        Player.You,
                        ZonePlacement.Shuffled,
                    )
                )
            },
        ) then Effects.ForEachPlayer(Player.Each, Effects.DrawCards(7))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a49dc44-616e-4bdd-8220-0bb71eccc512.jpg?1783948700"
        ruling(
            "2013-07-01",
            "This card won’t be put into your graveyard until after it’s finished resolving, which means " +
                "it won’t be shuffled into your library as part of its own effect.",
        )
    }
}

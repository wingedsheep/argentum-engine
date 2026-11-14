package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Day's Undoing
 * {2}{U}
 * Sorcery
 * Each player shuffles their hand and graveyard into their library, then draws seven cards. If it's
 * your turn, end the turn.
 *
 * Timetwister's body — every player shuffles hand and graveyard into their library (one gather across
 * both zones, one shuffled move) before anyone draws seven — followed by [Effects.EndTheTurn] gated on
 * [Conditions.IsYourTurn]. Ending the turn exiles the rest of the stack, this spell included, discards
 * any triggers the wheel produced, and skips to cleanup.
 */
val DaysUndoing = card("Day's Undoing") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Each player shuffles their hand and graveyard into their library, then draws seven cards. " +
        "If it's your turn, end the turn. (Exile all spells and abilities from the stack, including this " +
        "card. Discard down to your maximum hand size. Damage wears off, and \"this turn\" and \"until end " +
        "of turn\" effects end.)"

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
        ) then Effects.ForEachPlayer(Player.Each, Effects.DrawCards(7)) then
            Effects.If(Conditions.IsYourTurn, Effects.EndTheTurn)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "51"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a11287a-8002-4679-9f69-a7360e7f4d3c.jpg?1783938353"
        ruling(
            "2017-04-18",
            "If any abilities trigger while players are shuffling cards into their library or drawing seven " +
                "cards, those abilities cease to exist when the turn ends. They won't be put on the stack.",
        )
        ruling(
            "2015-06-22",
            "If both your hand and graveyard are empty as Day's Undoing starts resolving, you'll still " +
                "shuffle your library before drawing seven cards.",
        )
    }
}

package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Discovery // Dispersal (GRN 223) — split-layout spell (CR 709).
 *
 * Discovery {1}{U/B} — Sorcery
 *   Surveil 2, then draw a card.
 *
 * Dispersal {3}{U}{B} — Instant
 *   Each opponent returns a nonland permanent they control with the greatest mana value among
 *   permanents they control to its owner's hand, then discards a card.
 *
 * Lands have mana value 0, so "greatest among permanents they control" equals the greatest among
 * their nonland permanents; an opponent with only lands returns nothing but still discards
 * (2018-10-05 ruling). Inside [Effects.ForEachPlayer] the iterated opponent is `Player.You` and the
 * default chooser, so each opponent picks among their own ties. Every opponent returns first and only
 * then does each discard, matching the ruling's order — a card one opponent bounced to another's
 * hand is already there when that player discards.
 */
val DiscoveryDispersal = card("Discovery // Dispersal") {
    layout = CardLayout.SPLIT
    colorIdentity = "UB"

    face("Discovery") {
        manaCost = "{1}{U/B}"
        typeLine = "Sorcery"
        oracleText = "Surveil 2, then draw a card. (To surveil 2, look at the top two cards of your " +
            "library, then put any number of them into your graveyard and the rest on top of your " +
            "library in any order.)"

        spell {
            effect = Effects.Surveil(2) then Effects.DrawCards(1)
        }
    }

    face("Dispersal") {
        manaCost = "{3}{U}{B}"
        typeLine = "Instant"
        oracleText = "Each opponent returns a nonland permanent they control with the greatest mana " +
            "value among permanents they control to its owner's hand, then discards a card."

        spell {
            effect = Effects.ForEachPlayer(
                Player.EachOpponent,
                Effects.Pipeline {
                    val nonland = gather(
                        CardSource.BattlefieldMatching(
                            filter = GameObjectFilter.NonlandPermanent,
                            player = Player.You
                        )
                    )
                    val greatest = filter(nonland, CollectionFilter.GreatestManaValue)
                    val returned = chooseExactly(
                        1,
                        from = greatest,
                        useTargetingUI = true,
                        prompt = "Return a nonland permanent you control with the greatest mana value to its owner's hand"
                    )
                    move(returned, CardDestination.ToZone(Zone.HAND))
                }
            ) then Effects.EachOpponentDiscards(1)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ace631d1-897a-417e-8628-0170713f03d3.jpg?1783934113"
        ruling("2018-10-05", "If an opponent controls only lands as Dispersal resolves, that player won't return any permanent to their hand. They'll still discard a card.")
        ruling("2018-10-05", "If a permanent has {X} in its mana cost, X is considered to be 0.")
        ruling("2018-10-05", "As Dispersal resolves, first the next opponent in turn order (or, if it's an opponent's turn, that opponent) chooses a nonland permanent they control with the highest mana cost among their permanents, then each other opponent in turn order does the same, knowing the choices made before them. Then all the chosen permanents are returned at the same time. Next, each opponent in the same order chooses a card in hand without revealing it, then the chosen cards are discarded at the same time.")
    }
}

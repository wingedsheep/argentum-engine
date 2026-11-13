package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Fathom Feeder
 * {U}{B}
 * Creature — Eldrazi Drone
 * 1/1
 * Devoid (This card has no color.)
 * Deathtouch
 * Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)
 * {3}{U}{B}: Draw a card. Each opponent exiles the top card of their library.
 *
 * Ingest is engine-live: declaring the keyword is the whole ability (see `Ingest`). The activated
 * ability's exile is the same `Patterns.Library.exileTop(1)` the ingest trigger uses, run under
 * `ForEachPlayer(EachOpponent)` so "you" is rebound to each opponent: each card goes face up into
 * its owner's exile, where processors can put it into its owner's graveyard. An empty library
 * exiles nothing.
 */
val FathomFeeder = card("Fathom Feeder") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Eldrazi Drone"
    power = 1
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "Deathtouch\n" +
        "Ingest (Whenever this creature deals combat damage to a player, that player exiles the top card of their library.)\n" +
        "{3}{U}{B}: Draw a card. Each opponent exiles the top card of their library."

    keywords(Keyword.DEVOID, Keyword.DEATHTOUCH, Keyword.INGEST)

    activatedAbility {
        cost = Costs.Mana("{3}{U}{B}")
        effect = Effects.DrawCards(1) then
            Effects.ForEachPlayer(Player.EachOpponent, Patterns.Library.exileTop(1))
        description = "{3}{U}{B}: Draw a card. Each opponent exiles the top card of their library."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b080ed31-db68-4e3c-8bf6-48b53d3ba622.jpg?1783938182"
        ruling("2015-08-25", "The card exiled by the ingest ability is exiled face up.")
        ruling("2015-08-25", "If the player has no cards in their library when the ingest ability resolves, nothing happens. That player won't lose the game (until they have to draw a card from an empty library).")
    }
}

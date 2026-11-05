package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Old-Growth Dryads
 * {G}
 * Creature — Dryad
 * 3/3
 * When this creature enters, each opponent may search their library for a basic land card, put it
 * onto the battlefield tapped, then shuffle.
 *
 * [Effects.ForEachPlayer] over [Player.EachOpponent] rebinds `Player.You` to each opponent in turn
 * (the Field of Ruin shape), and the [Effects.May] inside it is that opponent's choice — declining
 * skips the search and the shuffle entirely.
 */
val OldGrowthDryads = card("Old-Growth Dryads") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, each opponent may search their library for a basic land card, " +
        "put it onto the battlefield tapped, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachPlayer(
            Player.EachOpponent,
            Effects.May(
                Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand,
                    count = 1,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true
                ),
                prompt = "Search your library for a basic land card and put it onto the battlefield tapped?"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "Yongjae Choi"
        flavorText = "\"The jungle was here before any city. It will be here after the last city falls.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06458882-94c8-4d92-a21c-955e9d70f0d4.jpg?1783935722"
    }
}

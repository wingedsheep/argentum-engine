package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Innocuous Rat
 * {1}{B}
 * Creature — Rat
 * 1/1
 * When this creature dies, manifest dread. (Look at the top two cards of your library. Put one
 * onto the battlefield face down as a 2/2 creature and the other into your graveyard. Turn it face
 * up any time for its mana cost if it's a creature card.)
 *
 * Dies-trigger flavor of the shared [Patterns.Library.manifestDread] recipe (CR 701.62b).
 */
val InnocuousRat = card("Innocuous Rat") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    oracleText = "When this creature dies, manifest dread. (Look at the top two cards of your " +
        "library. Put one onto the battlefield face down as a 2/2 creature and the other into your " +
        "graveyard. Turn it face up any time for its mana cost if it's a creature card.)"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Patterns.Library.manifestDread()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Maxime Minard"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94edbbc5-5673-4753-bfad-4432c4b7dca4.jpg?1726286232"
    }
}

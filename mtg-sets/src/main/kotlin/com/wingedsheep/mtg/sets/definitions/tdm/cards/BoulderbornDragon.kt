package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Boulderborn Dragon — Tarkir: Dragonstorm #239
 * {5} · Artifact Creature — Dragon · 3/3
 *
 * Flying, vigilance
 * Whenever this creature attacks, surveil 1. (Look at the top card of your library. You may put
 * it into your graveyard.)
 *
 * Flying and vigilance are keyword helpers; the attack trigger uses the atomic
 * [LibraryPatterns.surveil] composition.
 */
val BoulderbornDragon = card("Boulderborn Dragon") {
    manaCost = "{5}"
    typeLine = "Artifact Creature — Dragon"
    power = 3
    toughness = 3
    oracleText = "Flying, vigilance\n" +
        "Whenever this creature attacks, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = LibraryPatterns.surveil(1)
        description = "Whenever this creature attacks, surveil 1."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "239"
        artist = "Alexander Ostrowski"
        flavorText = "The draconic power that flowed out from the dragonstorms imbued the " +
            "landscape with draconic features—scales, claws, and appetites."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50c6e815-bfe7-4599-9227-d36504a3640f.jpg?1743204948"
    }
}

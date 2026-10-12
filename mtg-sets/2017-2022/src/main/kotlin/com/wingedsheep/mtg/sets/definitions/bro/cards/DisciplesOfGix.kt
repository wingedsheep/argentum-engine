package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Disciples of Gix
 * {4}{B}{B}
 * Creature — Phyrexian Human
 * 4/4
 * When this creature enters, search your library for up to three artifact cards, put them into
 * your graveyard, then shuffle.
 */
val DisciplesOfGix = card("Disciples of Gix") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Human"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, search your library for up to three artifact cards, " +
        "put them into your graveyard, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact,
            count = 3,
            destination = SearchDestination.GRAVEYARD
        )
        description = "When this creature enters, search your library for up to three artifact cards, " +
            "put them into your graveyard, then shuffle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Peter Polach"
        flavorText = "\"Machines feel no fear, no sorrow, no hunger—only glorious purpose, true and pure.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d356456-865d-4c92-8923-ce7384e29a79.jpg?1783920094"
    }
}

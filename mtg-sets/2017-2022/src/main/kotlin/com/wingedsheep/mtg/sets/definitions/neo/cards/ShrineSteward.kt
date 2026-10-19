package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Shrine Steward
 * {5}
 * Artifact Creature — Construct
 * 3/2
 * When this creature enters, you may search your library for an Aura or Shrine card, reveal it,
 * put it into your hand, then shuffle.
 */
val ShrineSteward = card("Shrine Steward") {
    manaCost = "{5}"
    typeLine = "Artifact Creature — Construct"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you may search your library for an Aura or Shrine card, " +
        "reveal it, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.withAnySubtype("Aura", "Shrine"),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "259"
        artist = "kleinerHai"
        flavorText = "From broken swords, the first Imperials forged a peaceful servant to tend the shrines of Eiganjo."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75e8d71c-3a0b-4042-a0f7-e99e92a79dc2.jpg?1783923820"
    }
}

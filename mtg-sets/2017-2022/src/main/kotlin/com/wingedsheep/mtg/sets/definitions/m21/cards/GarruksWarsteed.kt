package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Garruk's Warsteed
 * {3}{G}{G}
 * Creature — Rhino
 * 3/5
 * Vigilance
 * When this creature enters, you may search your library and/or graveyard for a card named Garruk,
 * Savage Herald, reveal it, and put it into your hand. If you search your library this way,
 * shuffle.
 *
 * The "you may" is the "Don't search" option of
 * [Patterns.Library.searchLibraryAndOrGraveyard], so the player answers one prompt, not a yes/no
 * followed by a zone choice.
 */
val GarruksWarsteed = card("Garruk's Warsteed") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rhino"
    power = 3
    toughness = 5
    oracleText = "Vigilance\nWhen this creature enters, you may search your library and/or graveyard for a " +
        "card named Garruk, Savage Herald, reveal it, and put it into your hand. If you search your " +
        "library this way, shuffle."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Garruk, Savage Herald"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this creature enters, you may search your library and/or graveyard for a " +
            "card named Garruk, Savage Herald, reveal it, and put it into your hand. If you search " +
            "your library this way, shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "337"
        artist = "Ilse Gort"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6099863-8d3d-44bf-8d3b-dc3602119617.jpg?1783930617"
    }
}

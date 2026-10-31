package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Rowan's Stalwarts
 * {4}{R}
 * Creature — Human Knight
 * 5/2
 * When this creature enters, you may search your library and/or graveyard for a card named
 * Rowan, Fearless Sparkmage, reveal it, and put it into your hand. If you search your library
 * this way, shuffle.
 *
 * Elspeth's Devotee's [Patterns.Library.searchLibraryAndOrGraveyard], naming Rowan.
 */
val RowansStalwarts = card("Rowan's Stalwarts") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Knight"
    power = 5
    toughness = 2
    oracleText = "When this creature enters, you may search your library and/or graveyard for a card " +
        "named Rowan, Fearless Sparkmage, reveal it, and put it into your hand. If you search your " +
        "library this way, shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Rowan, Fearless Sparkmage"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this creature enters, you may search your library and/or graveyard for a " +
            "card named Rowan, Fearless Sparkmage, reveal it, and put it into your hand. If you search " +
            "your library this way, shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "307"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b537c4e3-88d1-430d-b4c8-16e0716d1927.jpg?1783932555"
    }
}

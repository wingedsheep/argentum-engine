package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Chandra's Firemaw
 * {3}{R}{R}
 * Creature — Hellion
 * 4/2
 * Haste
 * When this creature enters, you may search your library and/or graveyard for a card named Chandra,
 * Flame's Catalyst, reveal it, and put it into your hand. If you search your library this way,
 * shuffle.
 */
val ChandrasFiremaw = card("Chandra's Firemaw") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Hellion"
    power = 4
    toughness = 2
    oracleText = "Haste\nWhen this creature enters, you may search your library and/or graveyard for a " +
        "card named Chandra, Flame's Catalyst, reveal it, and put it into your hand. If you search your " +
        "library this way, shuffle."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Chandra, Flame's Catalyst"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this creature enters, you may search your library and/or graveyard for a " +
            "card named Chandra, Flame's Catalyst, reveal it, and put it into your hand. If you search " +
            "your library this way, shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "333"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76cf0b50-155f-4e65-9e48-88b378ad93a1.jpg?1783930618"
    }
}

package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Teferi's Wavecaster
 * {3}{U}{U}
 * Creature — Merfolk Wizard
 * 3/3
 * Flash
 * When this creature enters, you may search your library and/or graveyard for a card named Teferi,
 * Timeless Voyager, reveal it, and put it into your hand. If you search your library this way,
 * shuffle.
 */
val TeferisWavecaster = card("Teferi's Wavecaster") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    power = 3
    toughness = 3
    oracleText = "Flash\nWhen this creature enters, you may search your library and/or graveyard for a " +
        "card named Teferi, Timeless Voyager, reveal it, and put it into your hand. If you search your " +
        "library this way, shuffle."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Teferi, Timeless Voyager"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this creature enters, you may search your library and/or graveyard for a " +
            "card named Teferi, Timeless Voyager, reveal it, and put it into your hand. If you search " +
            "your library this way, shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "327"
        artist = "Miranda Meeks"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d67e4c42-a44f-456b-87e7-21ca56dc4630.jpg?1783930621"
    }
}

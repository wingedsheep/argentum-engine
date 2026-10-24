package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Quandrix Cultivator — Strixhaven: School of Mages #218 (canonical printing)
 * {1}{G}{G/U}{U} · Creature — Turtle Druid · 3/4
 *
 * When this creature enters, you may search your library for a basic Forest or Island card, put
 * it onto the battlefield, then shuffle.
 *
 * The Quirion Trailblazer shape — an optional ETB library search — with a basic-land filter
 * narrowed to Forest or Island, entering untapped.
 */
val QuandrixCultivator = card("Quandrix Cultivator") {
    manaCost = "{1}{G}{G/U}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Turtle Druid"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, you may search your library for a basic Forest or " +
        "Island card, put it onto the battlefield, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand.withAnySubtype("Forest", "Island"),
                count = 1,
                destination = SearchDestination.BATTLEFIELD
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Filip Burburan"
        flavorText = "In the gardens spread across the Quandrix campus, fern turns to forest before " +
            "class is even over."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3ca9ff7-0dcf-4ecc-879c-957d290ad7f5.jpg?1783927300"
    }
}

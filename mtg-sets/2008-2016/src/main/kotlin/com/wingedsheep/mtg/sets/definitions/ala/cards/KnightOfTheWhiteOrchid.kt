package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Knight of the White Orchid
 * {W}{W}
 * Creature — Human Knight
 * 2/2
 * First strike
 * When this creature enters, if an opponent controls more lands than you, you may search your
 * library for a Plains card, put it onto the battlefield, then shuffle.
 *
 * The enters trigger is an intervening-if over [Conditions.OpponentControlsMoreLands]: it doesn't
 * trigger unless the condition holds, and does nothing if it no longer holds on resolution. The
 * Plains needn't be basic, and it enters untapped.
 */
val KnightOfTheWhiteOrchid = card("Knight of the White Orchid") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "First strike\n" +
        "When this creature enters, if an opponent controls more lands than you, you may search " +
        "your library for a Plains card, put it onto the battlefield, then shuffle."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.OpponentControlsMoreLands
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land.withSubtype(Subtype.PLAINS),
            destination = SearchDestination.BATTLEFIELD
        )
        description = "When this creature enters, if an opponent controls more lands than you, you " +
            "may search your library for a Plains card, put it onto the battlefield, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Mark Zug"
        flavorText = "Both guide and guard on open plain."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/642c6354-4dab-45c9-a1a6-57bd16f16e30.jpg?1783942580"

        ruling("2015-06-22", "The Plains you search for doesn't have to be basic. For example, you could put a Sacred Foundry onto the battlefield.")
        ruling("2008-10-01", "Knight of the White Orchid's triggered ability has an \"intervening 'if' clause.\" That means (1) the ability won't trigger at all unless any one of your opponents controls more lands than you, and (2) the ability will do nothing if you control at least as many lands as each of your opponents by the time it resolves.")
    }
}

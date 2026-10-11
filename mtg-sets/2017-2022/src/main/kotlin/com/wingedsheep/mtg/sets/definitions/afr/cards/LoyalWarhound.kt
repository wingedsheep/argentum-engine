package com.wingedsheep.mtg.sets.definitions.afr.cards

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
 * Loyal Warhound
 * {1}{W}
 * Creature — Dog
 * 3/1
 * Vigilance
 * When this creature enters, if an opponent controls more lands than you, search your library for
 * a basic Plains card, put it onto the battlefield tapped, then shuffle.
 *
 * The enters trigger is an intervening-if (CR 603.4) over the shared
 * [Conditions.OpponentControlsMoreLands] comparison, re-checked on resolution.
 */
val LoyalWarhound = card("Loyal Warhound") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dog"
    power = 3
    toughness = 1
    oracleText = "Vigilance\n" +
        "When this creature enters, if an opponent controls more lands than you, search your " +
        "library for a basic Plains card, put it onto the battlefield tapped, then shuffle."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.OpponentControlsMoreLands
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand.withSubtype(Subtype.PLAINS),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
        description = "When this creature enters, if an opponent controls more lands than you, " +
            "search your library for a basic Plains card, put it onto the battlefield tapped, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Dmitry Burmak"
        flavorText = "A paladin's steed is a celestial spirit in animal form."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f408c5e4-e18d-4e4d-959a-f41df4c3019c.jpg?1783926530"
    }
}

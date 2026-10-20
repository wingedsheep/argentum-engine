package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Land Tax
 * {W}
 * Enchantment
 * At the beginning of your upkeep, if an opponent controls more lands than you, you may search
 * your library for up to three basic land cards, reveal them, put them into your hand, then shuffle.
 *
 * The upkeep trigger is an intervening-if over the shared [Conditions.OpponentControlsMoreLands]
 * land comparison (checked on trigger and again on resolution); the "you may" makes the whole
 * search optional, and accepting it shuffles even when no basic land is taken.
 */
val LandTax = card("Land Tax") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, if an opponent controls more lands than you, you may search your library for up to three basic land cards, reveal them, put them into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.OpponentControlsMoreLands
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 3,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d53b20b0-67bc-4587-817b-efbf21cb2512.jpg?1783948083"
        ruling("2009-10-01", "You shuffle your library if you search, even if you don't put any basic land cards into your hand.")
        ruling("2009-10-01", "This ability has an \"intervening 'if' clause.\" That means (1) the ability won't trigger at all unless any one of your opponents controls more lands than you, and (2) the ability will do nothing if you control at least as many lands as each of your opponents by the time it resolves.")
        ruling("2006-10-15", "You can fetch any of the \"Snow-Covered\" lands, since they all also have the supertype basic.")
    }
}

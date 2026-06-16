package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Proctor's Gaze
 * {2}{G}{U}
 * Instant
 * Return up to one target nonland permanent to its owner's hand. Search your library for a basic
 * land card, put it onto the battlefield tapped, then shuffle.
 */
val ProctorsGaze = card("Proctor's Gaze") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Return up to one target nonland permanent to its owner's hand. Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."
    spell {
        val t = target(
            "up to one target nonland permanent",
            TargetPermanent(optional = true, filter = TargetFilter.NonlandPermanent)
        )
        effect = Effects.ReturnToHand(t)
            .then(
                Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true
                )
            )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Danny Schwartz"
        flavorText = "Only proctors are allowed to use magic in exam halls, enabling them to more easily catch cheaters in the act."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b127d543-0a90-4af6-9410-94d5cd30389e.jpg"
    }
}

package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Tezzeret, Artifice Master
 * {3}{U}{U}
 * Legendary Planeswalker — Tezzeret
 * Starting Loyalty: 5
 *
 * +1: Create a 1/1 colorless Thopter artifact creature token with flying.
 * 0: Draw a card. If you control three or more artifacts, draw two cards instead.
 * −9: You get an emblem with "At the beginning of your end step, search your library for a
 *     permanent card, put it onto the battlefield, then shuffle."
 */
val TezzeretArtificeMaster = card("Tezzeret, Artifice Master") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Tezzeret"
    startingLoyalty = 5
    oracleText = "+1: Create a 1/1 colorless Thopter artifact creature token with flying.\n" +
        "0: Draw a card. If you control three or more artifacts, draw two cards instead.\n" +
        "−9: You get an emblem with \"At the beginning of your end step, search your library for a permanent card, put it onto the battlefield, then shuffle.\""

    // +1: Create a 1/1 colorless Thopter artifact creature token with flying.
    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/d/0/d0044cd9-2136-423e-aa2a-829e8f007535.jpg?1783934464",
        )
    }

    // 0: Draw a card. If you control three or more artifacts, draw two cards instead.
    // The artifact count is checked at resolution.
    loyaltyAbility(0) {
        effect = Effects.If(
            condition = Conditions.YouControlAtLeast(3, GameObjectFilter.Artifact),
            then = Effects.DrawCards(2),
            otherwise = Effects.DrawCards(1),
        )
    }

    // −9: Emblem — a permanent, sourceless "at the beginning of your end step" trigger.
    loyaltyAbility(-9) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.beginningOf(Step.END),
                effect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.Permanent,
                    destination = SearchDestination.BATTLEFIELD,
                ),
            ),
            descriptionOverride = "At the beginning of your end step, search your library for a permanent card, put it onto the battlefield, then shuffle.",
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "79"
        artist = "Josh Hass"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5e12371-f05c-41cf-92ca-7cb17c2f7f1a.jpg?1783934576"
        ruling("2018-07-13", "If you put a permanent onto the battlefield with Tezzeret's emblem's ability, any triggered abilities of that permanent that trigger during your end step won't trigger during that same end step.")
    }
}

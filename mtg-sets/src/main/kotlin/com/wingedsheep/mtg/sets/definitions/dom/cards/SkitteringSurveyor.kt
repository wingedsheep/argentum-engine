package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Skittering Surveyor
 * {3}
 * Artifact Creature — Construct
 * 1/2
 * When this creature enters, you may search your library for a basic land card,
 * reveal it, put it into your hand, then shuffle.
 */
val SkitteringSurveyor = card("Skittering Surveyor") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, you may search your library for a basic land card, reveal it, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            LibraryPatterns.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "230"
        artist = "Dan Murayama Scott"
        flavorText = "\"Like a cross between a spider and a spyglass, but friendlier.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90c7bea0-79c9-4856-8279-cef7cee82fc1.jpg?1562739519"
    }
}

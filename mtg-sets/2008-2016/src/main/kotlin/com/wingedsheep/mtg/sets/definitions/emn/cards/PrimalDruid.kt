package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Primal Druid
 * {1}{G}
 * Creature — Human Druid
 * 0/3
 * When this creature dies, you may search your library for a basic land card, put it onto the
 * battlefield tapped, then shuffle.
 */
val PrimalDruid = card("Primal Druid") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    oracleText = "When this creature dies, you may search your library for a basic land card, put it " +
        "onto the battlefield tapped, then shuffle."
    power = 0
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = Filters.BasicLand,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Tomasz Jedruszek"
        flavorText = "\"The power of the woods is only ever borrowed.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e928f694-c53b-46c5-8390-fa24b53f559c.jpg"
    }
}

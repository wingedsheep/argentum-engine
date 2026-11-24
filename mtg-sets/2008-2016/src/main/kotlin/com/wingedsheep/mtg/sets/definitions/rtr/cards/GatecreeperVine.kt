package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Gatecreeper Vine — Return to Ravnica #124
 * {1}{G} · Creature — Plant · 0/2
 *
 * Defender
 * When this creature enters, you may search your library for a basic land card or a Gate card,
 * reveal it, put it into your hand, then shuffle.
 */
val GatecreeperVine = card("Gatecreeper Vine") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant"
    power = 0
    toughness = 2
    oracleText = "Defender\nWhen this creature enters, you may search your library for a basic land card " +
        "or a Gate card, reveal it, put it into your hand, then shuffle."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand or GameObjectFilter.Land.withSubtype("Gate"),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Trevor Claxton"
        flavorText = "Every inch of Ravnica is home to something."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dabcc2f-7536-44e3-a495-bbfc526fdc5d.jpg?1783940349"
    }
}

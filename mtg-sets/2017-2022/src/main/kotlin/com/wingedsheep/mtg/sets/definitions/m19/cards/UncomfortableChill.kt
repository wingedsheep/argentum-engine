package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Uncomfortable Chill
 * {2}{U}
 * Instant
 * Creatures your opponents control get -2/-0 until end of turn.
 * Draw a card.
 */
val UncomfortableChill = card("Uncomfortable Chill") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Creatures your opponents control get -2/-0 until end of turn.\nDraw a card."

    spell {
        effect = Patterns.Group.modifyStatsForAll(
            power = -2,
            toughness = 0,
            filter = GroupFilter.AllCreaturesOpponentsControl
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Izzy"
        flavorText = "The cold slowed their movements until only their panicked eyeballs swiveled beneath the ice."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f12d70b-fff7-4d0c-982e-2fea70018a78.jpg?1783934576"
    }
}

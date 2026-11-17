package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Refocus
 * {1}{U}
 * Instant
 * Untap target creature.
 * Draw a card.
 */
val Refocus = card("Refocus") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Untap target creature.\nDraw a card."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Untap(creature) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Kev Walker"
        flavorText = "\"Before you can open your third eye, you must prove you can open the first two.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35c78973-f2ae-4c76-802f-793d1022fcbd.jpg?1783938704"
    }
}

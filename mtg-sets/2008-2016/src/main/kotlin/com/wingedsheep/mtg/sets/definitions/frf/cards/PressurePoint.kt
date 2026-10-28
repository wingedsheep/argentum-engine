package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pressure Point
 * {1}{W}
 * Instant
 * Tap target creature.
 * Draw a card.
 *
 * If the target is illegal on resolution the whole spell fizzles, so no card is drawn.
 */
val PressurePoint = card("Pressure Point") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Tap target creature.\nDraw a card."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Tap(t) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Chase Stone"
        flavorText = "By studying dragon anatomy, Jeskai monks have learned how to locate clusters of nerves " +
            "that will incapacitate even the mightiest dragons."
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0027360-736c-4b3e-b2cd-0d821cca587a.jpg?1783938712"
    }
}

package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Waterbending Lesson
 * {3}{U}
 * Sorcery — Lesson
 * Draw three cards. Then discard a card unless you waterbend {2}. (While paying a waterbend cost,
 * you can tap your artifacts and creatures to help. Each one pays for {1}.)
 *
 * The "unless you waterbend {2}" is an in-resolution waterbend payment gate
 * ([Effects.UnlessYouWaterbend]): after the draw the caster may pay a waterbend {2} — mana and/or
 * tapping their untapped artifacts and creatures, each paying {1} — and if they decline or can't
 * pay, they discard a card. The gate reuses the shared waterbend tap-to-help machinery (the same
 * one Ward—Waterbend and the spell/ability waterbend costs use).
 */
val WaterbendingLesson = card("Waterbending Lesson") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery — Lesson"
    oracleText = "Draw three cards. Then discard a card unless you waterbend {2}. " +
        "(While paying a waterbend cost, you can tap your artifacts and creatures to help. " +
        "Each one pays for {1}.)"

    spell {
        effect = Effects.Composite(
            Effects.DrawCards(3),
            Effects.UnlessYouWaterbend(amount = 2, otherwise = Effects.Discard(1))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Sylvain Sarrailh"
        flavorText = "\"Just push and pull the water—like this.\"\n—Katara"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f81e3b5-a0a9-4764-8c2d-b499ce1740b4.jpg?1764120543"
    }
}

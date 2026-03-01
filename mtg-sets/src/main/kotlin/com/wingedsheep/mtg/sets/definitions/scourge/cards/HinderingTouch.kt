package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Rarity

/**
 * Hindering Touch
 * {3}{U}
 * Instant
 * Counter target spell unless its controller pays {2}.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.
 * You may choose new targets for the copies.)
 */
val HinderingTouch = card("Hindering Touch") {
    manaCost = "{3}{U}"
    typeLine = "Instant"
    oracleText = "Counter target spell unless its controller pays {2}.\nStorm (When you cast this spell, copy it for each spell cast before it this turn. You may choose new targets for the copies.)"

    spell {
        target = Targets.Spell
        effect = Effects.CounterUnlessPays("{2}")
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Glen Angus"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db9735d9-4aac-4175-8ec8-fc9bfd8f2c5c.jpg?1562535667"
    }
}

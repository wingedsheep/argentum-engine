package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hold at Bay
 * {1}{W}
 * Instant
 *
 * Prevent the next 7 damage that would be dealt to any target this turn.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val HoldAtBay = card("Hold at Bay") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Prevent the next 7 damage that would be dealt to any target this turn."

    spell {
        val t = target(Targets.Any)
        effect = Effects.PreventNextDamage(7, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Nils Hamm"
        flavorText = "It was Daeron's bravery that brought him to Odunos. But it was the magic of Meletis that brought him home."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffeb407d-6a42-488a-9869-83da3047e45d.jpg?1783939579"
    }
}

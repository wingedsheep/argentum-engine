package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Whispering Wizard
 * {3}{U}
 * Creature — Human Wizard
 * 3/2
 * Whenever you cast a noncreature spell, create a 1/1 white Spirit creature token with flying.
 * This ability triggers only once each turn.
 */
val WhisperingWizard = card("Whispering Wizard") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "Whenever you cast a noncreature spell, create a 1/1 white Spirit creature token with flying. This ability triggers only once each turn."
    power = 3
    toughness = 2
    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        oncePerTurn = true
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING)
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "E. M. Gist"
        flavorText = "Ivold was aghast when his device revealed the full scope of the geist infestation."
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54fb422d-71f2-44ed-9589-32630ab87050.jpg?1782703130"
    }
}

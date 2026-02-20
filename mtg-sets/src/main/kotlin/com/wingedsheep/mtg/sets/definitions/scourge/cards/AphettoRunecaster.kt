package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.triggers.OnTurnFaceUp

/**
 * Aphetto Runecaster
 * {3}{U}
 * Creature — Human Wizard
 * 2/3
 * Whenever a permanent is turned face up, you may draw a card.
 */
val AphettoRunecaster = card("Aphetto Runecaster") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 3
    oracleText = "Whenever a permanent is turned face up, you may draw a card."

    triggeredAbility {
        trigger = OnTurnFaceUp(selfOnly = false)
        effect = MayEffect(DrawCardsEffect(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Eric Peterson"
        flavorText = "Unraveling the mysteries of a morph is its own reward."
        imageUri = "https://cards.scryfall.io/large/front/a/9/a926db3e-1a73-4e0c-beda-7ccf439da48a.jpg?1562533279"
    }
}

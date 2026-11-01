package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Jace's Triumph — War of the Spark #55 (canonical printing)
 * {2}{U}
 * Sorcery
 * Draw two cards. If you control a Jace planeswalker, draw three cards instead.
 *
 * "Instead" changes the count, not the event: one draw whose amount is a
 * [DynamicAmounts.conditional], checked on resolution (the Chandra's Triumph shape).
 */
val JacesTriumph = card("Jace's Triumph") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Draw two cards. If you control a Jace planeswalker, draw three cards instead."

    spell {
        effect = Effects.DrawCards(
            DynamicAmounts.conditional(
                condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Jace")),
                ifTrue = 3,
                ifFalse = 2
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Kieran Yanner"
        flavorText = "His triumph was not in outsmarting Bolas's plan, but in understanding why ultimate power is self-defeating."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/4924f177-5ee2-4d3f-9603-4b9aef4dac09.jpg?1783933463"
    }
}

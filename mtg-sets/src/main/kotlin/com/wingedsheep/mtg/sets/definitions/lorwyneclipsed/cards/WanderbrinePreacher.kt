package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wanderbrine Preacher
 * {1}{W}
 * Creature — Merfolk Cleric
 * 2/2
 *
 * Whenever this creature becomes tapped, you gain 2 life.
 */
val WanderbrinePreacher = card("Wanderbrine Preacher") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Merfolk Cleric"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature becomes tapped, you gain 2 life."

    triggeredAbility {
        trigger = Triggers.BecomesTapped
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Warren Mahy"
        flavorText = "\"Truly, I am sustained by the faith of my congregation.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3fc3f5f2-5a83-4358-8f23-42f26f345140.jpg?1767732504"
    }
}

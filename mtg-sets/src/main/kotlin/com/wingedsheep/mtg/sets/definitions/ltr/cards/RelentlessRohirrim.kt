package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Relentless Rohirrim
 * {3}{R}
 * Creature — Human Knight
 * 4/3
 *
 * When this creature enters, the Ring tempts you.
 */
val RelentlessRohirrim = card("Relentless Rohirrim") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Knight"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, the Ring tempts you."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.TheRingTemptsYou()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "John Di Giovanni"
        flavorText = "\"The Orcs are in the Deep! Helm! Helm! Forth Helmingas!\"\n—Gamling the Old"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21b7606a-6a4c-4bf0-b311-1883383161d2.jpg?1686969129"
    }
}

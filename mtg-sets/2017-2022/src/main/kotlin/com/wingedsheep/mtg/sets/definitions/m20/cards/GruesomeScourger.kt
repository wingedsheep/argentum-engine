package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gruesome Scourger (M20 #104)
 * {3}{B}{B}  Creature — Orc Warrior  3/3
 *
 * When this creature enters, it deals damage to target opponent or planeswalker equal to the
 * number of creatures you control.
 */
val GruesomeScourger = card("Gruesome Scourger") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Warrior"
    oracleText = "When this creature enters, it deals damage to target opponent or planeswalker " +
        "equal to the number of creatures you control."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(Targets.OpponentOrPlaneswalker)
        effect = Effects.DealDamage(DynamicAmounts.creaturesYouControl(), t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Karl Kopinski"
        flavorText = "His whips twist and tremble like living things, moved by the fury of the horde he commands."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0ffbb50e-76ca-4bfd-b6d1-95ab8cf3bbf8.jpg"
    }
}

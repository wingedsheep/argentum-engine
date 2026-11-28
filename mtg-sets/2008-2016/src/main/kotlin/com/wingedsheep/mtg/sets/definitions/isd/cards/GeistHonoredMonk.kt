package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Geist-Honored Monk
 * {3}{W}{W}
 * Creature — Human Monk
 * * / *
 * Vigilance
 * Geist-Honored Monk's power and toughness are each equal to the number of creatures you control.
 * When this creature enters, create two 1/1 white Spirit creature tokens with flying.
 */
val GeistHonoredMonk = card("Geist-Honored Monk") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Monk"
    oracleText = "Vigilance\nGeist-Honored Monk's power and toughness are each equal to the number of creatures you control.\nWhen this creature enters, create two 1/1 white Spirit creature tokens with flying."
    keywords(Keyword.VIGILANCE)
    dynamicStats(DynamicAmounts.creaturesYouControl())
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            count = 2,
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d51355e-55fa-43bb-a5de-fc55ac7b6446.jpg"
    }
}

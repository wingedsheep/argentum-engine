package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Outflank
 * {W}
 * Instant
 *
 * Outflank deals damage to target attacking or blocking creature equal to the number of creatures
 * you control.
 */
val Outflank = card("Outflank") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Outflank deals damage to target attacking or blocking creature equal to the number of creatures you control."

    spell {
        val creature = target(TargetFilter.AttackingOrBlockingCreature)
        effect = Effects.DealDamage(DynamicAmounts.creaturesYouControl(), creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Victor Adame Minguez"
        flavorText = "\"With the drum of hooves and a flash of blades, the monster's terrifying roar changed to a cry of fear.\"\n" +
            "—*Legend of the Gilded Knights*"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/663e0208-81c3-4a76-bcc2-bc59cf8ca649.jpg"
    }
}

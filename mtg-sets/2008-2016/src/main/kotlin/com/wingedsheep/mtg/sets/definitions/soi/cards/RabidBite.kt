package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rabid Bite
 * {1}{G}
 * Sorcery
 *
 * Target creature you control deals damage equal to its power to target creature you don't control.
 */
val RabidBite = card("Rabid Bite") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature you control deals damage equal to its power to target creature you don't control."

    spell {
        val myCreature = target(TargetFilter.CreatureYouControl)
        val theirCreature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.powerOf(myCreature),
            target = theirCreature,
            damageSource = myCreature
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "223"
        artist = "Karl Kopinski"
        flavorText = "\"You should have seen the look on *both* their faces.\"\n—Morkamp, Lambholt innkeeper"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f573622-877b-4d21-adfc-40a32b7c2e6d.jpg?1783937725"
    }
}

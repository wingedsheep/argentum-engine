package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tail Slash
 * {2}{R}
 * Instant
 *
 * Target creature you control deals damage equal to its power to target creature you don't control.
 */
val TailSlash = card("Tail Slash") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
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
        collectorNumber = "161"
        artist = "Efrem Palacios"
        flavorText = "\"Kneel before a dragon and you will be spared when it turns to leave.\"\n—Yikaro, Atarka warrior"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91a59f12-8d1b-4378-abb8-e266d9b6cdba.jpg?1783938585"
        ruling("2015-02-25", "If either creature is an illegal target as Tail Slash tries to resolve, the creature you control won’t deal damage.")
    }
}

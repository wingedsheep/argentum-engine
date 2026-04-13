package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Rabid Gnaw
 * {1}{R}
 * Instant
 *
 * Target creature you control gets +1/+0 until end of turn. Then it deals
 * damage equal to its power to target creature you don't control.
 */
val RabidGnaw = card("Rabid Gnaw") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +1/+0 until end of turn. Then it deals damage equal to its power to target creature you don't control."

    spell {
        val myCreature = target("creature you control", Targets.CreatureYouControl)
        val theirCreature = target("creature you don't control", Targets.CreatureOpponentControls)
        effect = CompositeEffect(
            listOf(
                Effects.ModifyStats(1, 0, myCreature),
                Effects.DealDamage(
                    amount = DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.Power),
                    target = theirCreature,
                    damageSource = myCreature
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Mark Behm"
        flavorText = "Unbeknownst to the hosts, the foraged seeds they served at dinner had been touched by a Calamity Beast. The results were ... disturbing."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f815bae-820a-49f6-8eed-46f658e7b6ff.jpg?1721426681"
        ruling("2024-07-26", "If the creature you control is a legal target but the creature you don't control isn't as Rabid Gnaw resolves, the creature you control will get +1/+0 until end of turn, but no damage will be dealt. If instead the creature you control is an illegal target but the creature you don't control is still a legal target, nothing will happen when Rabid Gnaw resolves.")
    }
}

package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Friendly Rivalry
 * {R}{G}
 * Instant
 *
 * Target creature you control and up to one other target legendary creature you control
 * each deal damage equal to their power to target creature you don't control.
 */
val FriendlyRivalry = card("Friendly Rivalry") {
    manaCost = "{R}{G}"
    colorIdentity = "GR"
    typeLine = "Instant"
    oracleText = "Target creature you control and up to one other target legendary creature you control each deal damage equal to their power to target creature you don't control."

    spell {
        // Target 0: creature you control
        val myCreature = target("creature you control", Targets.CreatureYouControl)
        // Target 1: up to one *other* legendary creature you control. Wrapped in TargetOther so
        // "other" means "other than the index-0 creature" (CR 601.2c) — the same permanent can't
        // fill both slots and deal its power twice.
        val legendary = target(
            "up to one other legendary creature you control",
            TargetOther(
                baseRequirement = TargetCreature(
                    optional = true,
                    filter = TargetFilter.CreatureYouControl.legendary()
                )
            )
        )
        // Target 2: creature you don't control
        val theirCreature = target("creature you don't control", Targets.CreatureOpponentControls)

        effect = Effects.DealDamage(
            amount = DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.Power),
            target = theirCreature,
            damageSource = myCreature
        ).then(
            Effects.DealDamage(
                amount = DynamicAmount.EntityProperty(EntityReference.Target(1), EntityNumericProperty.Power),
                target = theirCreature,
                damageSource = legendary
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Marc Simonetti"
        flavorText = "\"Forty-two, Master Legolas!\"\n—Gimli"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24076763-88ba-4dee-b548-9c27bd34fdb7.jpg?1686969776"
    }
}

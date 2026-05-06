package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Hard-Hitting Question
 * {G}
 * Sorcery
 * Target creature you control deals damage equal to its power to target creature or planeswalker you don't control.
 */
val HardHittingQuestion = card("Hard-Hitting Question") {
    manaCost = "{G}"
    typeLine = "Sorcery"
    oracleText = "Target creature you control deals damage equal to its power to target creature or planeswalker you don't control."

    spell {
        val myCreature = target("creature you control", Targets.CreatureYouControl)
        val theirTarget = target("creature or planeswalker you don't control", TargetPermanent(
            filter = TargetFilter(com.wingedsheep.sdk.scripting.GameObjectFilter.CreatureOrPlaneswalker.opponentControls())
        ))
        effect = DealDamageEffect(
            amount = DynamicAmount.EntityProperty(
                EntityReference.Target(0),
                EntityNumericProperty.Power
            ),
            target = theirTarget,
            damageSource = myCreature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "164"
        artist = "Nicholas Gregory"
        flavorText = "\"Keen eyes and an analytical mind are some of a detective's most important tools. As is a mean left hook.\"\n—Senior Inspector Holjo"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8ad807c2-14a7-4464-bf57-c323fb3c0bd0.jpg?1706242021"
    }
}

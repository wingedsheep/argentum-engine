package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep


/**
 * Harsh Justice
 * {2}{W}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * This turn, whenever an attacking creature deals combat damage to you, it deals that much damage to its controller.
 */
val HarshJustice = card("Harsh Justice") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Cast this spell only during the declare attackers step and only if you've been attacked this step.\nThis turn, whenever an attacking creature deals combat damage to you, it deals that much damage to its controller."
    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)
        effect = Effects.CreateDelayedTrigger(
            trigger = Triggers.a(GameObjectFilter.Creature.attacking()).dealsCombatDamage(to = Recipient.You),
            effect = Effects.DealDamage(
                DynamicAmounts.triggerDamageAmount(),
                EffectTarget.ControllerOfTriggeringEntity,
                damageSource = EffectTarget.TriggeringEntity,
            ),
            fireOnce = false,
        )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "John Coulthart"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3657001a-7f79-4d3f-9d35-462ecf684fa8.jpg?1783946849"
    }
}

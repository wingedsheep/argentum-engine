package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Delif's Cone
 * {0}
 * Artifact
 * {T}, Sacrifice this artifact: This turn, when target creature you control attacks and isn't
 * blocked, you may gain life equal to its power. If you do, it assigns no combat damage this turn.
 *
 * "This turn, when …" is a one-shot delayed trigger watching the chosen creature (CR 603.7a): it
 * waits for that creature's unblocked attack, fires at most once, and is gone at end of turn
 * whether or not it fired. The life gained reads the creature's power as the trigger resolves,
 * so a pump between declare-blockers and resolution counts.
 */
val DelifsCone = card("Delif's Cone") {
    manaCost = "{0}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice this artifact: This turn, when target creature you control " +
        "attacks and isn't blocked, you may gain life equal to its power. If you do, it assigns " +
        "no combat damage this turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        val t = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.youControl()))
        )
        effect = CreateDelayedTriggerEffect(
            trigger = Triggers.AttacksAndIsntBlocked.copy(binding = TriggerBinding.ANY),
            watchedTarget = t,
            effect = MayEffect(
                Effects.Composite(
                    Effects.GainLife(
                        DynamicAmount.EntityProperty(
                            EntityReference.Triggering,
                            EntityNumericProperty.Power
                        )
                    ),
                    GrantKeywordEffect(
                        AbilityFlag.ASSIGNS_NO_COMBAT_DAMAGE.name,
                        EffectTarget.TriggeringEntity,
                        Duration.EndOfTurn,
                    ),
                ),
                descriptionOverride = "gain life equal to that creature's power. If you do, it assigns no combat damage this turn",
            ),
            expiry = DelayedTriggerExpiry.EndOfTurn,
            fireOnce = true,
        )
        description = "{T}, Sacrifice this artifact: This turn, when target creature you control attacks and isn't blocked, you may gain life equal to its power. If you do, it assigns no combat damage this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Mark Tedin"
        flavorText = "\"Where is it written that beasts must cause pain?\"\n—Delif, *Ponderings*"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/262b8788-c5a0-4c8e-9d58-b769b1b0a2ff.jpg?1783947881"
    }
}

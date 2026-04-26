package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Giantfall
 * {1}{R}
 * Instant
 *
 * Choose one —
 * • Target creature you control deals damage equal to its power to target creature an opponent controls.
 * • Destroy target artifact.
 */
val Giantfall = card("Giantfall") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Target creature you control deals damage equal to its power to target creature an opponent controls.\n• Destroy target artifact."

    spell {
        effect = ModalEffect.chooseOne(
            Mode(
                effect = Effects.DealDamage(
                    amount = DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.Power),
                    target = EffectTarget.ContextTarget(1),
                    damageSource = EffectTarget.ContextTarget(0)
                ),
                targetRequirements = listOf(Targets.CreatureYouControl, Targets.CreatureOpponentControls),
                description = "Target creature you control deals damage equal to its power to target creature an opponent controls"
            ),
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                Targets.Artifact,
                "Destroy target artifact"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Drew Baker"
        flavorText = "Bump went the giant. Gone went the hamlet."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1ac52728-adb3-4220-8392-73f7bd379ab4.jpg?1767957191"
    }
}

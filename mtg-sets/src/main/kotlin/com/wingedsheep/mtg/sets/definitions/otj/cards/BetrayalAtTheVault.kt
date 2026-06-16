package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Betrayal at the Vault — Outlaws of Thunder Junction #155
 * {4}{G}{G} · Instant · Uncommon
 *
 * Target creature you control deals damage equal to its power to each of two other
 * target creatures.
 *
 * Three target requirements: the source creature you control (index 0), then two
 * "other target creatures" (indices 1 and 2). Each recipient is wrapped in
 * [TargetOther] so it must differ from every earlier chosen target — index 1 differs
 * from the source, index 2 differs from both the source and index 1 (CR 601.2c +
 * "other"). Both [Effects.DealDamage] read the source's power at resolution via
 * [DynamicAmount.EntityProperty] on `Target(0)` and attribute the damage to the source
 * creature via `damageSource`.
 *
 * Scryfall rulings (2024-04-12): if the source creature has left the battlefield before
 * resolution the spell does nothing (the source is itself a target, so the spell is
 * countered for having an illegal target). If only one of the two recipients has left,
 * the source still deals damage to the remaining recipient — the unset/illegal target
 * resolves to a no-op deal-damage.
 */
val BetrayalAtTheVault = card("Betrayal at the Vault") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control deals damage equal to its power to each of two other target creatures."

    spell {
        // Target 0: the source — a creature you control.
        target("creature you control", Targets.CreatureYouControl)
        // Targets 1 & 2: two other target creatures (distinct from the source and each other).
        target(
            "first other target creature",
            TargetOther(baseRequirement = TargetObject(filter = TargetFilter.Creature))
        )
        target(
            "second other target creature",
            TargetOther(baseRequirement = TargetObject(filter = TargetFilter.Creature))
        )

        val sourcePower = DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.Power)
        effect = Effects.DealDamage(
            amount = sourcePower,
            target = EffectTarget.ContextTarget(1),
            damageSource = EffectTarget.ContextTarget(0)
        ).then(
            Effects.DealDamage(
                amount = sourcePower,
                target = EffectTarget.ContextTarget(2),
                damageSource = EffectTarget.ContextTarget(0)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Andreas Zafiratos"
        flavorText = "The vault cracked open, and the unlikely alliance met the likeliest of ends, as " +
            "greed, rage, and self-interest won out over any shred of loyalty."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c494820f-607d-4ed2-8a86-a916ae390272.jpg?1712355886"
    }
}

package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Electrostatic Bolt — Mirrodin #89
 * {R} · Instant
 *
 * Electrostatic Bolt deals 2 damage to target creature. If it's an artifact creature,
 * Electrostatic Bolt deals 4 damage to it instead.
 *
 * "Instead" replaces the amount, not the event, so this is one damage event whose amount is a
 * [DynamicAmount.Conditional] (the Bring Low shape) rather than two branching damage effects. The
 * artifact check is resolution-time: a creature that becomes an artifact in response is dealt 4,
 * and one that loses artifact-ness in response is dealt 2.
 */
val ElectrostaticBolt = card("Electrostatic Bolt") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Electrostatic Bolt deals 2 damage to target creature. If it's an artifact creature, " +
        "Electrostatic Bolt deals 4 damage to it instead."

    spell {
        target = Targets.Creature
        effect = Effects.DealDamage(
            amount = DynamicAmount.Conditional(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.ArtifactCreature),
                ifTrue = DynamicAmount.Fixed(4),
                ifFalse = DynamicAmount.Fixed(2)
            ),
            target = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Randy Gallegos"
        flavorText = "It's hard to avoid electric shock when the entire plane is metallic."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c455c8d4-6f20-4dcb-8e82-c2bb70d6bc3e.jpg?1783944541"
    }
}

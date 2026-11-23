package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Homing Lightning
 * {2}{R}{R}
 * Instant
 * Homing Lightning deals 4 damage to target creature and each other creature with the same name as
 * that creature.
 *
 * Bile Blight's shape with damage: the target is hit directly, then every other creature whose name
 * matches the target's (read as the spell resolves, whoever controls it) is hit in turn. A
 * face-down creature has no name (CR 708.2a) and shares it with nothing, so a face-down target is
 * damaged alone and a face-down namesake is never swept up (Declaration in Stone's guard).
 */
val HomingLightning = card("Homing Lightning") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Homing Lightning deals 4 damage to target creature and each other creature with the same " +
        "name as that creature."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(4, creature) then Effects.If(
            Conditions.TargetMatchesFilter(GameObjectFilter.Creature.faceUp(), creature),
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.faceUp().sharingNameWith(creature), excludeTarget = true),
                Effects.DealDamage(4, EffectTarget.IterationEntity)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Slawomir Maniak"
        flavorText = "\"I see countless worlds. What makes them think I can't find them in just one?\"\n—Ral Zarek"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6535816-8fa4-4c8b-8677-ac80f769f528.jpg"
    }
}

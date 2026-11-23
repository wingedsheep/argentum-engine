package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Izzet Staticaster — Return to Ravnica #173 (canonical printing)
 * {1}{U}{R} · Creature — Human Wizard · 0/3
 *
 * Flash
 * Haste
 * {T}: This creature deals 1 damage to target creature and each other creature with the same name
 * as that creature.
 *
 * Homing Lightning's shape on a tap ability: the target is hit directly, then every other creature
 * whose name matches the target's (read on resolution, whoever controls it) is hit in turn. A
 * face-down creature has no name (CR 708.2a), so a face-down target is damaged alone and a
 * face-down namesake is never swept up.
 */
val IzzetStaticaster = card("Izzet Staticaster") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Wizard"
    power = 0
    toughness = 3
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\nHaste\n" +
        "{T}: This creature deals 1 damage to target creature and each other creature with the same " +
        "name as that creature."

    keywords(Keyword.FLASH, Keyword.HASTE)

    activatedAbility {
        cost = Costs.Tap
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(1, creature) then Effects.If(
            Conditions.TargetMatchesFilter(GameObjectFilter.Creature.faceUp(), creature),
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.faceUp().sharingNameWith(creature), excludeTarget = true),
                Effects.DealDamage(1, EffectTarget.IterationEntity)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "173"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/190ac2fe-532d-4d7e-9d74-07ae6850aac8.jpg?1783940338"
    }
}

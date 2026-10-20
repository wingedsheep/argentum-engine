package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glyph of Destruction — Legends #150
 * {R} · Instant
 *
 * Target blocking Wall you control gets +10/+0 until end of combat. Prevent all damage that would
 * be dealt to it this turn. Destroy it at the beginning of the next end step.
 *
 * A pump for [Duration.EndOfCombat] (Murk Dwellers' shape), Indestructible Aura's plain
 * prevention shield, and Nettling Imp's step-based delayed destroy at [Step.END] whose
 * `watchedTarget` bakes in the Wall.
 */
val GlyphOfDestruction = card("Glyph of Destruction") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target blocking Wall you control gets +10/+0 until end of combat. Prevent all damage that " +
        "would be dealt to it this turn. Destroy it at the beginning of the next end step."

    spell {
        val wall = target(
            TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.WALL).blocking().youControl())
        )
        effect = Effects.ModifyStats(10, 0, wall, Duration.EndOfCombat) then
            Effects.PreventDamage(target = wall) then
            Effects.CreateDelayedTrigger(
                step = Step.END,
                watchedTarget = wall,
                effect = Effects.Destroy(EffectTarget.TriggeringEntity)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Susan Van Camp"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e9c153c-9224-491b-bc84-8a9f0a83ee5a.jpg?1783948056"
    }
}

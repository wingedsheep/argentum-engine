package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glyph of Doom — Legends #100
 * {B} · Instant
 *
 * Choose target Wall creature. At this turn's next end of combat, destroy all creatures that were
 * blocked by that creature this turn.
 *
 * Gaze of the Gorgon's shape, one-directional: a step-based delayed trigger at [Step.END_COMBAT]
 * ([DelayedTriggerTiming.THIS_TURN_ONLY] — cast after the turn's last combat it never fires) whose
 * `watchedTarget` bakes in the Wall, and a sweep over every creature whose turn-scoped block
 * record says the Wall blocked it ([GameObjectFilter.wasBlockedByThisTurn] over
 * [EffectTarget.TriggeringEntity]). Blocks made before the Glyph was cast count, and the Wall
 * needn't still be on the battlefield when the trigger resolves.
 */
val GlyphOfDoom = card("Glyph of Doom") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose target Wall creature. At this turn's next end of combat, destroy all creatures that " +
        "were blocked by that creature this turn."

    spell {
        val wall = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Wall")))
        effect = Effects.CreateDelayedTrigger(
            step = Step.END_COMBAT,
            timing = DelayedTriggerTiming.THIS_TURN_ONLY,
            watchedTarget = wall,
            effect = Effects.DestroyAll(
                GameObjectFilter.Creature.wasBlockedByThisTurn(EffectTarget.TriggeringEntity)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "100"
        artist = "Susan Van Camp"
        flavorText = "\"He knows he has a short span of life, that the day will come when he must pass " +
            "through the wall of oblivion . . .\"\n—William Faulkner"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/332bfce9-052d-42e9-a407-4a1dd59e0f2a.jpg"
    }
}

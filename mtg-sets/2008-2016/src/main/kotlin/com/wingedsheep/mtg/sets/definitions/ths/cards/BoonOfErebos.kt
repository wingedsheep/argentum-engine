package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boon of Erebos
 * {B}
 * Instant
 * Target creature gets +2/+0 until end of turn. Regenerate it. You lose 2 life.
 */
val BoonOfErebos = card("Boon of Erebos") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+0 until end of turn. Regenerate it. You lose 2 life."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, creature) then
            Effects.Regenerate(creature) then
            Effects.LoseLife(2, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "James Ryman"
        flavorText = "\"Death is not a certainty. Erebos determines when a mortal's time is up.\"\n—Iadorna, death priest of Erebos"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/4371083b-c300-4464-a7ea-6168db4e345c.jpg?1783939782"
    }
}

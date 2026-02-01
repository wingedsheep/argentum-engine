package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.CreatureTargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Smother
 * {1}{B}
 * Instant
 * Destroy target creature with mana value 3 or less. It can't be regenerated.
 *
 * Note: The "can't be regenerated" clause is not yet implemented as regeneration
 * is not yet supported in the engine.
 */
val Smother = card("Smother") {
    manaCost = "{1}{B}"
    typeLine = "Instant"

    spell {
        target = TargetCreature(filter = CreatureTargetFilter.WithManaValueAtMost(3))
        effect = DestroyEffect(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Karl Kopinski"
        flavorText = "\"Before I hire new recruits, I test how long they can hold their breath. You'd be surprised how often it comes up.\"\n—Zahr Gada, Halimar expedition leader"
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf605c8e-e59f-4f49-bb55-1824e7eadec0.jpg"
    }
}

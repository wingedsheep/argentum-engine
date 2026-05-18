package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Restless Dead
 * {1}{B}
 * Creature — Skeleton
 * 1/1
 * {B}: Regenerate this creature.
 */
val RestlessDead = card("Restless Dead") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Skeleton"
    power = 1
    toughness = 1
    oracleText = "{B}: Regenerate this creature."

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = RegenerateEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Ian Miller"
        flavorText = "The rich's heirs often thank them after death . . . but preferably not in person.\n—Suq'Ata epigram"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a237cff4-af6f-4745-bda1-e3ed2267fa89.jpg?1562720951"
    }
}

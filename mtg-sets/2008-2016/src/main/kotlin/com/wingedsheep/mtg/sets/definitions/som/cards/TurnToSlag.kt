package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Turn to Slag — Scars of Mirrodin #106 (canonical printing)
 * {3}{R}{R} · Sorcery
 *
 * Turn to Slag deals 5 damage to target creature. Destroy all Equipment attached to that creature.
 *
 * Fires of Mount Doom's damage-then-[Effects.DestroyAllEquipmentOnTarget] pair. State-based
 * actions don't run mid-resolution, so the creature is still on the battlefield (with its
 * Equipment attached) when the destroy reads it. A creature with no Equipment is a legal target.
 */
val TurnToSlag = card("Turn to Slag") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Turn to Slag deals 5 damage to target creature. Destroy all Equipment attached to that creature."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(5, creature) then
            Effects.DestroyAllEquipmentOnTarget(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Zoltan Boros & Gabor Szikszai"
        flavorText = "\"If it wasn't a blackened, stinking, melted abomination before, it certainly is now.\"\n—Koth of the Hammer"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66fd5b49-b4f2-40da-94d5-6d6fc69506f6.jpg?1783941721"
        ruling("2020-06-23", "Turn to Slag can target a creature that has no Equipment attached to it.")
        ruling("2020-06-23", "If the target creature is an illegal target by the time Turn to Slag tries to resolve, the spell won't resolve. You won't destroy any Equipment.")
    }
}

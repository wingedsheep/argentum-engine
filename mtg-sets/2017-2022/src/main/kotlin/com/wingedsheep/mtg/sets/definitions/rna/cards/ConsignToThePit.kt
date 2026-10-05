package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Consign to the Pit
 * {5}{B}
 * Sorcery
 * Destroy target creature. Consign to the Pit deals 2 damage to that creature's controller.
 *
 * "That creature's controller" is [EffectTarget.TargetController], which falls back to
 * last-known information once the creature has been destroyed — and still finds the controller
 * when the creature survives (indestructible), per the ruling.
 */
val ConsignToThePit = card("Consign to the Pit") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. Consign to the Pit deals 2 damage to that creature's controller."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then Effects.DealDamage(2, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Colin Boyer"
        flavorText = "First your whole life flashes before your eyes. Then you have considerable time to reflect on every regret as you plummet."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09991fad-4282-4a17-bfb1-03eaa13502df.jpg?1783933695"
        ruling("2019-01-25", "If the target creature is an illegal target by the time Consign to the Pit tries to resolve, the spell doesn't resolve. No player is dealt damage. If the target is legal but not destroyed (most likely because it has indestructible), its controller is dealt damage.")
    }
}

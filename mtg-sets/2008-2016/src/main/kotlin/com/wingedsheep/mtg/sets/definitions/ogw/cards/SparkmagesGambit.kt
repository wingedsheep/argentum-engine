package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sparkmage's Gambit
 * {1}{R}
 * Sorcery
 * Sparkmage's Gambit deals 1 damage to each of up to two target creatures. Those creatures can't
 * block this turn.
 *
 * Two passes over the same still-legal targets: the atomic `DealDamage` body deals its damage as one
 * simultaneous instruction (Dual Shot), then each target gets the can't-block restriction
 * (Glarewielder). The restriction doesn't depend on the damage, so it still applies when the damage
 * is prevented or redirected; an illegal target is skipped by both passes.
 */
val SparkmagesGambit = card("Sparkmage's Gambit") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Sparkmage's Gambit deals 1 damage to each of up to two target creatures. " +
        "Those creatures can't block this turn."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(Effects.DealDamage(1, EffectTarget.ContextTarget(0))) then
            Effects.ForEachTarget(Effects.CantBlock(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Mathias Kollros"
        flavorText = "\"A touch of lightning, and anyone can be taught to dance.\"\n—Tobin, sparkmage"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/736ad6c8-e275-4644-9d1e-d2d380149839.jpg?1783937905"
        ruling(
            "2016-01-22",
            "If Sparkmage's Gambit has two targets, and one of them is illegal as Sparkmage's Gambit " +
                "resolves, only the remaining legal target will be affected. The illegal target won't be " +
                "dealt damage and will be able to block that turn. If both targets are illegal as " +
                "Sparkmage's Gambit tries to resolve, it won't resolve and none of its effects will happen."
        )
        ruling(
            "2016-01-22",
            "If Sparkmage's Gambit resolves, but the damage is prevented or redirected, the target " +
                "creatures still won't be able to block that turn."
        )
    }
}

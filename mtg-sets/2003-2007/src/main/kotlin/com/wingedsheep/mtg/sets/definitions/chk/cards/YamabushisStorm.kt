package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yamabushi's Storm
 * {1}{R}
 * Sorcery
 * Yamabushi's Storm deals 1 damage to each creature. If a creature dealt damage this way would die
 * this turn, exile it instead.
 *
 * Yamabushi's Flame's shape fanned over every creature: each visited creature gets the
 * [Effects.MarkExileOnDeath] mark *before* its 1 damage, so a creature killed by the Storm itself
 * is exiled, and the mark lasts the rest of the turn.
 */
val YamabushisStorm = card("Yamabushi's Storm") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Yamabushi's Storm deals 1 damage to each creature. If a creature dealt damage this way " +
        "would die this turn, exile it instead."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature),
            Effects.MarkExileOnDeath(EffectTarget.IterationEntity) then
                Effects.DealDamage(1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "199"
        artist = "Wayne England"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a5a930d-ae59-47e2-9b98-f703e308b5c0.jpg?1783944293"
    }
}

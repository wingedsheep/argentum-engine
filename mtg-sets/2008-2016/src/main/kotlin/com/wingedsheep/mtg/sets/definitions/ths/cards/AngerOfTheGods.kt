package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Anger of the Gods
 * {1}{R}{R}
 * Sorcery
 * Anger of the Gods deals 3 damage to each creature. If a creature dealt damage this way would die
 * this turn, exile it instead.
 *
 * Same shape as Yamabushi's Storm: each creature gets the [Effects.MarkExileOnDeath] mark before its
 * damage, so a creature killed by this spell (or later this turn) is exiled instead.
 */
val AngerOfTheGods = card("Anger of the Gods") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Anger of the Gods deals 3 damage to each creature. If a creature dealt damage this way " +
        "would die this turn, exile it instead."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature),
            Effects.MarkExileOnDeath(EffectTarget.IterationEntity) then
                Effects.DealDamage(3, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90795891-5e67-47c0-8d52-a5e5c5a9ef81.jpg?1783939769"
    }
}

package com.wingedsheep.mtg.sets.definitions.vis.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Relentless Assault
 * {2}{R}{R}
 * Sorcery
 * Untap all creatures that attacked this turn. After this main phase, there is an additional
 * combat phase followed by an additional main phase.
 */
val RelentlessAssault = card("Relentless Assault") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Untap all creatures that attacked this turn. After this main phase, there is an additional combat phase followed by an additional main phase."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.attackedThisTurn()),
            Effects.Untap(EffectTarget.IterationEntity)
        ) then Effects.AddCombatPhase then Effects.AddMainPhase
    }

    metadata {
        flavorText = "Flog and Squee / Up the tree / See the army / Flee, flee, flee.\n—Goblin nursery rhyme/war cry"
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Geofrey Darrow & I. Rabarot"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/747161ea-cb65-4960-84dd-a05bfe5f3ba0.jpg?1783946986"
    }
}

package com.wingedsheep.mtg.sets.definitions.vis.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crypt Rats
 * {2}{B}
 * Creature — Rat
 * 1/1
 * {X}: This creature deals X damage to each creature and each player. Spend only black mana on X.
 *
 * "Each creature and each player" is the Thrashing Wumpus idiom: a group pass over the creatures
 * and a player pass. "Spend only black mana on X" is the ability's `xManaRestriction`, honored by
 * the mana solver and the activated-ability payment path (same as Atalya, Samite Master).
 */
val CryptRats = card("Crypt Rats") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    oracleText = "{X}: This creature deals X damage to each creature and each player. Spend only black mana on X."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{X}")
        xManaRestriction = setOf(Color.BLACK)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature),
            Effects.DealDamage(DynamicAmounts.xValue(), EffectTarget.IterationEntity)
        ) then
            Effects.ForEachPlayer(
                Player.Each,
                listOf(Effects.DealDamage(DynamicAmounts.xValue(), EffectTarget.Controller))
            )
        description = "{X}: This creature deals X damage to each creature and each player. " +
            "Spend only black mana on X."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Paul Lee"
        flavorText = "\"Once I dreamt of death, but now it dreams of me / And only rats and rotting flesh can hear my silent plea.\"\n—Mundungu chant"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/736455f6-c1b3-4a5a-a91f-a0cd3986ed53.jpg?1783946994"
    }
}

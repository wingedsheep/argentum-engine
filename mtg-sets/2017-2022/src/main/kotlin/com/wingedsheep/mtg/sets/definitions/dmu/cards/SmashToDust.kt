package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Smash to Dust
 * {1}{R}
 * Sorcery
 * Choose one —
 * • Destroy target artifact.
 * • Destroy target creature with defender.
 * • Smash to Dust deals 1 damage to each creature your opponents control.
 */
val SmashToDust = card("Smash to Dust") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Destroy target artifact.\n" +
        "• Destroy target creature with defender.\n" +
        "• Smash to Dust deals 1 damage to each creature your opponents control."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target artifact") {
                val t = target(TargetFilter.Artifact)
                effect = Effects.Destroy(t)
            }
            mode("Destroy target creature with defender") {
                val t = target(TargetFilter(GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER)))
                effect = Effects.Destroy(t)
            }
            mode("Smash to Dust deals 1 damage to each creature your opponents control") {
                effect = Effects.ForEachInGroup(
                    filter = GroupFilter.AllCreaturesOpponentsControl,
                    effect = Effects.DealDamage(1, EffectTarget.IterationEntity),
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Marc Simonetti"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/747881c7-3788-4dd6-b78d-b2a5ded20e14.jpg?1783921310"
    }
}

package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.entwineModal
import com.wingedsheep.sdk.dsl.entwine
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wail of the Nim
 * {2}{B}
 * Instant
 * Choose one —
 * • Regenerate each creature you control.
 * • Wail of the Nim deals 1 damage to each creature and each player.
 * Entwine {B} (Choose both if you pay the entwine cost.)
 */
val WailOfTheNim = card("Wail of the Nim") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Regenerate each creature you control.\n" +
        "• Wail of the Nim deals 1 damage to each creature and each player.\n" +
        "Entwine {B} (Choose both if you pay the entwine cost.)"

    entwine("{B}")

    spell {
        entwineModal {
            mode("Regenerate each creature you control") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl()),
                    Effects.Regenerate(EffectTarget.IterationEntity)
                )
            }
            mode("Wail of the Nim deals 1 damage to each creature and each player") {
                effect = Effects.ForEachInGroup(
                    GroupFilter.AllCreatures,
                    Effects.DealDamage(1, EffectTarget.IterationEntity)
                ) then Effects.ForEachPlayer(
                    Player.Each,
                    listOf(Effects.DealDamage(1, EffectTarget.Controller))
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "John Matson"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8c32faa-c6d1-418a-aed6-ccc5849daa1f.jpg?1783944544"
    }
}

package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.entwineModal
import com.wingedsheep.sdk.dsl.entwine
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Roar of the Kha
 * {1}{W}
 * Instant
 * Choose one —
 * • Creatures you control get +1/+1 until end of turn.
 * • Untap all creatures you control.
 * Entwine {1}{W} (Choose both if you pay the entwine cost.)
 */
val RoarOfTheKha = card("Roar of the Kha") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Creatures you control get +1/+1 until end of turn.\n" +
        "• Untap all creatures you control.\n" +
        "Entwine {1}{W} (Choose both if you pay the entwine cost.)"

    entwine("{1}{W}")

    spell {
        entwineModal {
            mode("Creatures you control get +1/+1 until end of turn") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl()),
                    Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
                )
            }
            mode("Untap all creatures you control") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl()),
                    Effects.Untap(EffectTarget.IterationEntity)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "18"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc697a4c-f219-46fd-90f2-63c638cd5ef7.jpg?1783944560"
    }
}

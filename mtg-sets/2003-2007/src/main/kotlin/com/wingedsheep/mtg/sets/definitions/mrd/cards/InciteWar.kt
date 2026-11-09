package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.entwineModal
import com.wingedsheep.sdk.dsl.entwine
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Targets

/**
 * Incite War
 * {2}{R}
 * Instant
 * Choose one —
 * • Creatures target player controls attack this turn if able.
 * • Creatures you control gain first strike until end of turn.
 * Entwine {2} (Choose both if you pay the entwine cost.)
 */
val InciteWar = card("Incite War") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Creatures target player controls attack this turn if able.\n" +
        "• Creatures you control gain first strike until end of turn.\n" +
        "Entwine {2} (Choose both if you pay the entwine cost.)"

    entwine("{2}")

    spell {
        entwineModal {
            mode("Creatures target player controls attack this turn if able") {
                target(Targets.Player)
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.targetPlayerControls()),
                    Effects.MarkMustAttackThisTurn(EffectTarget.IterationEntity),
                )
            }
            mode("Creatures you control gain first strike until end of turn") {
                effect = Patterns.Group.grantKeywordToAll(
                    Keyword.FIRST_STRIKE,
                    GroupFilter.AllCreaturesYouControl,
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Alex Horley-Orlandelli"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee30527c-0eb1-4b66-9028-1607a960019a.jpg?1783944540"
    }
}

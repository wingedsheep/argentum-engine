package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flame Sweep
 * {2}{R}
 * Instant
 * Flame Sweep deals 2 damage to each creature except for creatures you control with flying.
 *
 * "Each creature except creatures you control with flying" is the union of every creature without
 * flying and every creature an opponent controls — a heterogeneous `or`, so each branch keeps its
 * own controller gate.
 */
val FlameSweep = card("Flame Sweep") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Flame Sweep deals 2 damage to each creature except for creatures you control with flying."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(
                GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING) or
                    GameObjectFilter.Creature.opponentControls()
            ),
            Effects.DealDamage(2, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Viktor Titov"
        flavorText = "\"I have calculated that a dragon in flight, in a single strafing run, can cover an area of—\"\n—Thaedus the Clever, last words"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/4702a838-8577-49b6-8b7a-f9b6d2929481.jpg"
    }
}

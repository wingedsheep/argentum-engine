package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Goblin Battle Jester
 * {3}{R}
 * Creature — Goblin
 * 2/2
 * Whenever you cast a red spell, target creature can't block this turn.
 */
val GoblinBattleJester = card("Goblin Battle Jester") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "Whenever you cast a red spell, target creature can't block this turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withColor(Color.RED))
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantBlock(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "135"
        artist = "Steve Prescott"
        flavorText = "Somehow goblins found a tactical advantage by sending a clown to war."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c13e56b0-becc-4bc2-9ba3-23b3ca8bfe58.jpg"
    }
}

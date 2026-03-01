package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Lookout
 * {1}{R}
 * Creature — Goblin
 * 1/2
 * {T}, Sacrifice a Goblin: Goblin creatures get +2/+0 until end of turn.
 */
val GoblinLookout = card("Goblin Lookout") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin"
    power = 1
    toughness = 2
    oracleText = "{T}, Sacrifice a Goblin: Goblin creatures get +2/+0 until end of turn."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Goblin"))
        )
        effect = ForEachInGroupEffect(
            filter = GroupFilter.allCreaturesWithSubtype("Goblin"),
            effect = ModifyStatsEffect(2, 0, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Jim Nelson"
        flavorText = "\"Throw rocks at 'em! Throw spears at 'em! Throw Furt at 'em!\""
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23bbe84a-8857-467a-a4a1-e57086cc9501.jpg?1562902159"
    }
}

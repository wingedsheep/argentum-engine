package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Firebug
 * {1}{R}
 * Creature — Goblin
 * 2/2
 * When Goblin Firebug leaves the battlefield, sacrifice a land.
 */
val GoblinFirebug = card("Goblin Firebug") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 2
    oracleText = "When Goblin Firebug leaves the battlefield, sacrifice a land."

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.Sacrifice(GameObjectFilter.Land, count = 1, target = EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Christopher Moeller"
        flavorText = "Most goblins leave behind a wake of destruction. A few goblins take one with them."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2370d319-d1d2-4bca-9275-ff72fb400709.jpg?1562902107"
    }
}

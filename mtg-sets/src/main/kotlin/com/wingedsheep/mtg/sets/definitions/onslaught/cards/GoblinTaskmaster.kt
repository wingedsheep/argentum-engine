package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Goblin Taskmaster
 * {R}
 * Creature — Goblin
 * 1/1
 * {1}{R}: Target Goblin creature gets +1/+0 until end of turn.
 * Morph {R}
 */
val GoblinTaskmaster = card("Goblin Taskmaster") {
    manaCost = "{R}"
    typeLine = "Creature — Goblin"
    power = 1
    toughness = 1
    oracleText = "{1}{R}: Target Goblin creature gets +1/+0 until end of turn.\nMorph {R}"

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        val t = target("target", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Goblin"))
        ))
        effect = ModifyStatsEffect(1, 0, t)
    }

    morph = "{R}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Trevor Hairsine"
        flavorText = "\"For some reason, goblin fighting school isn't as crowded on day two.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/e/feff65ca-aedf-4434-b701-590d600d1a0b.jpg?1562955378"
    }
}

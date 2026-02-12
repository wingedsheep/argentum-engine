package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyAllEffect
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForGroupEffect

/**
 * Goblin Pyromancer
 * {3}{R}
 * Creature — Goblin Wizard
 * 2/2
 * When Goblin Pyromancer enters the battlefield, Goblin creatures get +3/+0 until end of turn.
 * At the beginning of the end step, destroy all Goblins.
 */
val GoblinPyromancer = card("Goblin Pyromancer") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Goblin Wizard"
    power = 2
    toughness = 2
    oracleText = "When Goblin Pyromancer enters the battlefield, Goblin creatures get +3/+0 until end of turn.\nAt the beginning of the end step, destroy all Goblins."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModifyStatsForGroupEffect(
            powerModifier = 3,
            toughnessModifier = 0,
            filter = GroupFilter.allCreaturesWithSubtype("Goblin")
        )
    }

    triggeredAbility {
        trigger = Triggers.EachEndStep
        effect = DestroyAllEffect(
            filter = GroupFilter.allCreaturesWithSubtype("Goblin")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"The good news is, we figured out how the wand works. The bad news is, we figured out how the wand works.\""
        imageUri = "https://cards.scryfall.io/large/front/b/b/bb4815b7-fc20-44a4-ad1c-66d92993557f.jpg?1562939185"
    }
}

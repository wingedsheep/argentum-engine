package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

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
        effect = ForEachInGroupEffect(
            filter = GroupFilter.allCreaturesWithSubtype("Goblin"),
            effect = ModifyStatsEffect(3, 0, EffectTarget.Self)
        )
    }

    triggeredAbility {
        trigger = Triggers.EachEndStep
        effect = ForEachInGroupEffect(
            filter = GroupFilter.allCreaturesWithSubtype("Goblin"),
            effect = MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, byDestruction = true)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "206"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"The good news is, we figured out how the wand works. The bad news is, we figured out how the wand works.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb4815b7-fc20-44a4-ad1c-66d92993557f.jpg?1562939185"
    }
}

package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Deepchannel Duelist
 * {W}{U}
 * Creature — Merfolk Soldier
 * 2/2
 *
 * At the beginning of your end step, untap target Merfolk you control.
 * Other Merfolk you control get +1/+1.
 */
val DeepchannelDuelist = card("Deepchannel Duelist") {
    manaCost = "{W}{U}"
    typeLine = "Creature — Merfolk Soldier"
    power = 2
    toughness = 2
    oracleText = "At the beginning of your end step, untap target Merfolk you control.\nOther Merfolk you control get +1/+1."

    triggeredAbility {
        trigger = Triggers.YourEndStep
        val merfolk = target("merfolk", TargetCreature(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Merfolk").youControl())
        ))
        effect = Effects.Untap(merfolk)
    }

    staticAbility {
        ability = ModifyStatsForCreatureGroup(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Merfolk"), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Richard Kane Ferguson"
        flavorText = "\"Bargain with ruthless cunning, and when all else fails, make sure you fight with the same.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b742172-7118-45e7-9945-62bd77d94e85.jpg?1767957310"
    }
}

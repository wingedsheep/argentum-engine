package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Keldon Warcaller
 * {1}{R}
 * Creature — Human Warrior
 * 2/2
 * Whenever Keldon Warcaller attacks, put a lore counter on target Saga you control.
 */
val KeldonWarcaller = card("Keldon Warcaller") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "Whenever Keldon Warcaller attacks, put a lore counter on target Saga you control."

    triggeredAbility {
        trigger = Triggers.Attacks
        val saga = target("Saga you control", TargetObject(
            filter = TargetFilter(GameObjectFilter.Enchantment.withSubtype("Saga").youControl())
        ))
        effect = Effects.AddCounters("lore", 1, saga)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Aaron Miller"
        flavorText = "\"The Mountain gave the Flame to Kradak to light the furnaces of his people's hearts. The wanderers became Keldons, and he the first warlord.\"\n—\"The Flame of Keld\""
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22d2e230-2d60-402d-98e4-0f33d2f27c56.jpg?1636491681"
    }
}

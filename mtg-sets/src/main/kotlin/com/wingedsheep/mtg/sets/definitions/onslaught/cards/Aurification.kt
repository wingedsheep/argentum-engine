package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.AddCreatureTypeByCounter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GrantKeywordByCounter
import com.wingedsheep.sdk.scripting.triggers.OnCreatureDealsDamageToYou
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.triggers.OnLeavesBattlefield
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect

/**
 * Aurification
 * {2}{W}{W}
 * Enchantment
 * Whenever a creature deals damage to you, put a gold counter on it.
 * Each creature with a gold counter on it is a Wall in addition to its
 * other creature types and has defender.
 * When Aurification leaves the battlefield, remove all gold counters
 * from all creatures.
 */
val Aurification = card("Aurification") {
    manaCost = "{2}{W}{W}"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature deals damage to you, put a gold counter on it.\nEach creature with a gold counter on it is a Wall in addition to its other creature types and has defender.\nWhen Aurification leaves the battlefield, remove all gold counters from all creatures."

    triggeredAbility {
        trigger = OnCreatureDealsDamageToYou
        effect = AddCountersEffect("gold", 1, EffectTarget.TriggeringEntity)
    }

    staticAbility { ability = AddCreatureTypeByCounter("Wall", "gold") }
    staticAbility { ability = GrantKeywordByCounter(Keyword.DEFENDER, "gold") }

    triggeredAbility {
        trigger = OnLeavesBattlefield(selfOnly = true)
        effect = ForEachInGroupEffect(GroupFilter.AllCreatures, RemoveCountersEffect("gold", Int.MAX_VALUE, EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Gary Ruddell"
        flavorText = "Trespassers will be decorated."
        imageUri = "https://cards.scryfall.io/large/front/9/3/93d9e9ea-9f88-4206-8960-b5ebe839ee16.jpg?1562929867"
    }
}

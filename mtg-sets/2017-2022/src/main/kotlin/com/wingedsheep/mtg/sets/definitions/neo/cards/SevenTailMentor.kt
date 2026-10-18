package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Seven-Tail Mentor — Kamigawa: Neon Dynasty #36 (canonical printing)
 * {3}{W} · Creature — Fox Samurai · 2/3
 *
 * When this creature enters or dies, put a +1/+1 counter on target creature or Vehicle you control.
 *
 * "Enters or dies" is two triggered abilities sharing one printed sentence, each choosing its own
 * target. "Creature or Vehicle" is [GameObjectFilter.CreatureOrVehicle], so an uncrewed Vehicle is a
 * legal target.
 */
val SevenTailMentor = card("Seven-Tail Mentor") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Fox Samurai"
    power = 2
    toughness = 3
    oracleText = "When this creature enters or dies, put a +1/+1 counter on target creature or Vehicle you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter(GameObjectFilter.CreatureOrVehicle.youControl()))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val t = target(TargetFilter(GameObjectFilter.CreatureOrVehicle.youControl()))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Eelis Kyttanen"
        flavorText = "\"I have taught you all I can. Take this blade and rise as my equal.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d77d1e6-c804-46f7-ba00-6df6bca2ae08.jpg?1783923912"
    }
}

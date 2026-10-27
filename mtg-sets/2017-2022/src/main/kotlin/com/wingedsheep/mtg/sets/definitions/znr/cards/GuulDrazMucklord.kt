package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Guul Draz Mucklord — Zendikar Rising #104
 * {2}{B} · Creature — Crocodile · 2/3
 *
 * When this creature dies, put a +1/+1 counter on target creature you control.
 */
val GuulDrazMucklord = card("Guul Draz Mucklord") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Crocodile"
    oracleText = "When this creature dies, put a +1/+1 counter on target creature you control."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.dies()
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Darek Zabrocki"
        flavorText = "Death comes in many forms on Guul Draz, most of them slow, subtle, and agonizing. " +
            "At least a monstrous crocodile offers a swift death."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b996743e-7f48-49d8-948b-ceb75bddf215.jpg"
    }
}

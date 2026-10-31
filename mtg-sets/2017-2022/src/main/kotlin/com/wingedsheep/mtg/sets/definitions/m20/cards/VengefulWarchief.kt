package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vengeful Warchief — Core Set 2020 #121
 * {4}{B} · Creature — Orc Warrior · 4/4 · Uncommon
 *
 * Whenever you lose life for the first time each turn, put a +1/+1 counter on this creature.
 * (Damage causes loss of life.)
 *
 * `Triggers.you.losesLife(firstTimeEachTurn = true)` fires only on the controller's first
 * life-loss event of the turn, whether from damage or a "loses life" effect.
 */
val VengefulWarchief = card("Vengeful Warchief") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Warrior"
    power = 4
    toughness = 4
    oracleText = "Whenever you lose life for the first time each turn, put a +1/+1 counter on this creature. (Damage causes loss of life.)"

    triggeredAbility {
        trigger = Triggers.you.losesLife(firstTimeEachTurn = true)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "121"
        artist = "Karl Kopinski"
        flavorText = "\"Try to steal from me and I'll add your skull to my collection.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d405e1bc-bf58-4cce-8abe-57fb40ef0996.jpg?1783932986"
    }
}

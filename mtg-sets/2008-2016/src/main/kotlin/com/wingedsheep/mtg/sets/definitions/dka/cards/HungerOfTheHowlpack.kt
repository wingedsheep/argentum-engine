package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hunger of the Howlpack
 * {G}
 * Instant
 * Put a +1/+1 counter on target creature.
 * Morbid — Put three +1/+1 counters on that creature instead if a creature died this turn.
 */
val HungerOfTheHowlpack = card("Hunger of the Howlpack") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature.\nMorbid — Put three +1/+1 counters on that creature instead if a creature died this turn."
    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.CreatureDiedThisTurn,
            then = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 3, target = t),
            otherwise = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = t),
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Nils Hamm"
        flavorText = "\"Werewolves are an unholy mix of a predator's instinct and a human's hatred.\"\n—Alena, trapper of Kessig"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b38a0dbc-3ebd-4f87-a5fb-bc2ee8a48a8d.jpg?1783940804"
    }
}

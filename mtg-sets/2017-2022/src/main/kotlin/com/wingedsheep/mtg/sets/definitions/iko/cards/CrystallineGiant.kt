package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Crystalline Giant — Ikoria: Lair of Behemoths #234
 * {3} · Artifact Creature — Giant · 3/3
 *
 * At the beginning of combat on your turn, choose a kind of counter at random that this creature
 * doesn't have on it from among flying, first strike, deathtouch, hexproof, lifelink, menace,
 * reach, trample, vigilance, and +1/+1. Put a counter of that kind on this creature.
 *
 * The kind is drawn at resolution among the listed kinds the Giant has none of, so a counter it
 * gained in response is excluded; with all ten already on it, nothing happens.
 */
val CrystallineGiant = card("Crystalline Giant") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Giant"
    power = 3
    toughness = 3
    oracleText = "At the beginning of combat on your turn, choose a kind of counter at random that this " +
        "creature doesn't have on it from among flying, first strike, deathtouch, hexproof, lifelink, " +
        "menace, reach, trample, vigilance, and +1/+1. Put a counter of that kind on this creature."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.AddCounterOfAbsentKind(
            kinds = listOf(
                CounterType.FLYING, CounterType.FIRST_STRIKE, CounterType.DEATHTOUCH, CounterType.HEXPROOF,
                CounterType.LIFELINK, CounterType.MENACE, CounterType.REACH, CounterType.TRAMPLE,
                CounterType.VIGILANCE, CounterType.PLUS_ONE_PLUS_ONE
            ),
            atRandom = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/1146c418-2176-466e-8a06-f2ef6bf2b1a9.jpg?1783931007"
    }
}

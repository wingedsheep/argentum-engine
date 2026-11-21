package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gleam of Battle
 * {4}{R}{W}
 * Enchantment
 *
 * Whenever a creature you control attacks, put a +1/+1 counter on it.
 *
 * Per-attacker trigger: one instance for each creature you declare as an attacker, each putting
 * its counter on the creature that fired it (Miriam, Herd Whisperer's shape).
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val GleamOfBattle = card("Gleam of Battle") {
    manaCost = "{4}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control attacks, put a +1/+1 counter on it."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Raymond Swanland"
        flavorText = "Every soldier returns from battle changed: by hardship, by blood, by a glimpse of glory."
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5f0feef-1a71-4c8c-9fd1-f5cbe718a988.jpg?1783940028"
    }
}

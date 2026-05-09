package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ajani's Pridemate
 * {1}{W}
 * Creature — Cat Soldier
 * 2/2
 *
 * Whenever you gain life, put a +1/+1 counter on this creature.
 */
val AjanisPridemate = card("Ajani's Pridemate") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Soldier"
    power = 2
    toughness = 2
    oracleText = "Whenever you gain life, put a +1/+1 counter on this creature."

    // Whenever you gain life, put a +1/+1 counter on this creature
    triggeredAbility {
        trigger = Triggers.YouGainLife
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Kevin Sidharta"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/222c1a68-e34c-4103-b1be-17d4ceaef6ce.jpg?1730489107"
    }
}

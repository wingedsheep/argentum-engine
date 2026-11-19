package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pheres-Band Tromper
 * {3}{G}
 * Creature — Centaur Warrior
 * 3/3
 *
 * Inspired — Whenever this creature becomes untapped, put a +1/+1 counter on it.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val PheresBandTromper = card("Pheres-Band Tromper") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Centaur Warrior"
    power = 3
    toughness = 3
    oracleText = "Inspired — Whenever this creature becomes untapped, put a +1/+1 counter on it."

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "134"
        artist = "Volkan Baǵa"
        flavorText = "Trompers are the most feared warriors of the Pheres Band. Anger is their lifeblood. " +
            "It sustains them and gives them purpose."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2d7e7d1-70ee-450b-8018-ff373cc91c68.jpg?1783939529"
    }
}

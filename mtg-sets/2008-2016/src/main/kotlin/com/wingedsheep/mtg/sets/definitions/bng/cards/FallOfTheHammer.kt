package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Fall of the Hammer — Born of the Gods #93 (canonical printing)
 * {1}{R}
 * Instant
 * Target creature you control deals damage equal to its power to another target creature.
 *
 * The Cosmic Hunger / Markov Retribution bite idiom: the biter is target 0, the victim is a
 * [TargetOther] creature (which may be one you control), damage equals the biter's power read at
 * resolution, and the biter is the damage source.
 */
val FallOfTheHammer = card("Fall of the Hammer") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature you control deals damage equal to its power to another target creature."

    spell {
        val biter = target(TargetFilter.Creature.youControl())
        val victim = target(TargetOther(TargetObject(filter = TargetFilter.Creature)))
        effect = Effects.DealDamage(DynamicAmounts.powerOf(biter), victim, damageSource = biter)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Adam Paquette"
        flavorText = "It is unwise to insult the god of the forge. It is also unwise to stand near anyone " +
            "else who has insulted him."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed6f1a4b-db3b-4dfc-8d3a-cc73b011a5dd.jpg?1783939546"
    }
}

package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Procrastinate
 * {X}{U}
 * Sorcery
 *
 * Tap target creature. Put twice X stun counters on it.
 *
 * Tap the chosen creature, then drop a dynamic number of stun counters on it: twice the X
 * paid (`Multiply(XValue, 2)`). The stun-counter rules (a stunned permanent removes a
 * counter instead of untapping) are engine-handled; this just places them.
 */
val Procrastinate = card("Procrastinate") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Tap target creature. Put twice X stun counters on it. (If a permanent with a " +
        "stun counter would become untapped, remove one from it instead.)"

    spell {
        val creature = target("creature", Targets.Creature)
        effect = Effects.Tap(creature)
            .then(
                Effects.AddDynamicCounters(
                    Counters.STUN,
                    DynamicAmount.Multiply(DynamicAmount.XValue, 2),
                    creature
                )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Elizabeth Peiró"
        flavorText = "\"None of these projects are due until end of semester. I've still got a " +
            "whole week to finish them!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1edb449d-620f-4e21-9d76-2c840635eb9d.jpg?1775937356"
    }
}

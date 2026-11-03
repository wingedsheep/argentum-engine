package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * I'm a Doctor, Not a . . .
 * {G}
 * Instant
 *
 * Put a +1/+1 counter on target creature you control. Untap it. Until end of turn, it gets +1/+1
 * and becomes a Doctor. (It loses all other creature types.)
 *
 * "Becomes a Doctor" overwrites creature subtypes in layer 4 until end of turn; card types,
 * colors and abilities are untouched.
 */
val ImADoctorNotA = card("I'm a Doctor, Not a . . .") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature you control. Untap it. Until end of turn, " +
        "it gets +1/+1 and becomes a Doctor. (It loses all other creature types.)"

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.Untap(creature) then
            Effects.ModifyStats(1, 1, creature) then
            Effects.SetCreatureSubtypes(setOf(Subtype.DOCTOR.value), creature, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Joel Chaim Holtzman"
        flavorText = "\"Dammit, Jim. We've been over this!\"\n—Dr. Leonard McCoy"
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c07979d4-8a58-4cfb-bffa-74e8bd97c7ff.jpg?1785981585"
    }
}

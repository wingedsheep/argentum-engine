package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Set Phasers to . . .
 * {W}
 * Instant
 *
 * Choose one —
 * • Stun — Tap target creature and put a stun counter on it.
 * • Kill — This spell deals 3 damage to target attacking creature.
 *
 * Named modes render as "Name — text", the house shape for named modal options.
 */
val SetPhasersTo = card("Set Phasers to . . .") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Stun — Tap target creature and put a stun counter on it. (If a permanent with a stun " +
        "counter would become untapped, remove one from it instead.)\n" +
        "• Kill — This spell deals 3 damage to target attacking creature."

    spell {
        modal(chooseCount = 1) {
            mode("Stun — Tap target creature and put a stun counter on it") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.Tap(creature) then
                    Effects.AddCounters(CounterType.STUN, 1, creature)
            }

            mode("Kill — This spell deals 3 damage to target attacking creature") {
                val attacker = target(TargetFilter.AttackingCreature)
                effect = Effects.DealDamage(3, attacker)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Elizabeth Peiró"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54dbf416-973d-46c8-b9be-e863283d328b.jpg?1784036750"
    }
}

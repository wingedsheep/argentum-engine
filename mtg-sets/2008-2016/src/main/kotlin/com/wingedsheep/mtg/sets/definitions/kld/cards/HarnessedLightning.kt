package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Harnessed Lightning — Kaladesh #117
 * {1}{R} · Instant
 *
 * Choose target creature. You get {E}{E}{E} (three energy counters), then you may pay any amount
 * of {E}. Harnessed Lightning deals that much damage to that creature.
 *
 * Galvanic Discharge's shape: [Effects.PayCounters] asks for 0..current energy on resolution and
 * stores the amount paid, which the damage reads back. An illegal target fizzles the whole spell
 * (no energy gained), via the standard resolution target check.
 */
val HarnessedLightning = card("Harnessed Lightning") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose target creature. You get {E}{E}{E} (three energy counters), then you may pay " +
        "any amount of {E}. Harnessed Lightning deals that much damage to that creature."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GetEnergy(3) then
            Effects.PayCounters(CounterType.ENERGY, storeAmountAs = "paid") then
            Effects.DealDamage(DynamicAmounts.storedNumber("paid"), t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9be9b5c-eed4-4a5d-962f-3482da5a6f1d.jpg?1783937194"
        ruling(
            "2016-09-20",
            "If the target creature becomes an illegal target, Harnessed Lightning doesn't resolve and none " +
                "of its effects happen. You won't get {E}{E}{E} or be able to pay any {E}."
        )
        ruling(
            "2016-09-20",
            "You choose the target creature as you cast Harnessed Lightning. You don't choose how much {E} " +
                "to pay until Harnessed Lightning is resolving. No player may take actions between the time " +
                "you choose how much {E} to pay and the time the creature takes damage equal to the {E} paid this way."
        )
    }
}

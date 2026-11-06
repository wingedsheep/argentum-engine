package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Die Young — Kaladesh #76
 * {1}{B} · Sorcery
 *
 * Choose target creature. You get {E}{E} (two energy counters), then you may pay any amount of
 * {E}. The creature gets -1/-1 until end of turn for each {E} paid this way.
 *
 * Galvanic Discharge's shape: [Effects.PayCounters] asks for 0..current energy on resolution and
 * stores the amount paid, which the -X/-X reads back. An illegal target fizzles the whole spell
 * (no energy gained), via the standard resolution target check.
 */
val DieYoung = card("Die Young") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose target creature. You get {E}{E} (two energy counters), then you may pay any " +
        "amount of {E}. The creature gets -1/-1 until end of turn for each {E} paid this way."

    spell {
        val t = target(TargetFilter.Creature)
        val paid = -DynamicAmounts.storedNumber("paid")
        effect = Effects.GetEnergy(2) then
            Effects.PayCounters(CounterType.ENERGY, storeAmountAs = "paid") then
            Effects.ModifyStats(power = paid, toughness = paid, target = t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Ryan Yee"
        flavorText = "\"When the time comes, let go. Nothing lasts forever.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daf7bb52-013f-4d8d-b4ea-53d1fa4bb694.jpg?1783937210"
        ruling(
            "2016-09-20",
            "If the target creature becomes an illegal target, Die Young doesn't resolve and none of its " +
                "effects happen. You won't get {E}{E} or be able to pay any {E}."
        )
        ruling(
            "2016-09-20",
            "You choose the target creature as you cast Die Young. You don't choose how much {E} to pay " +
                "until Die Young is resolving. No player may take actions between the time you choose how " +
                "much {E} to pay and the time the creature gets -1/-1 for each {E} paid this way."
        )
    }
}

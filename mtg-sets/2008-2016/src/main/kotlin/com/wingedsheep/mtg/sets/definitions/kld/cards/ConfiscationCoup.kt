package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Confiscation Coup — Kaladesh #41
 * {3}{U}{U} · Sorcery
 *
 * Choose target artifact or creature. You get {E}{E}{E}{E} (four energy counters), then you may pay
 * an amount of {E} equal to that permanent's mana value. If you do, gain control of it.
 *
 * Volatile Stormdrake's shape: [Effects.MayPay] over an exact [Effects.PayExactCounters] of the
 * target's resolution-time mana value. The gate only offers the payment when you have enough {E},
 * and a mana value of 0 (tokens, {X} permanents) is a free "yes". An illegal target fizzles the
 * whole spell (no energy gained), via the standard resolution target check.
 */
val ConfiscationCoup = card("Confiscation Coup") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Choose target artifact or creature. You get {E}{E}{E}{E} (four energy counters), then you " +
        "may pay an amount of {E} equal to that permanent's mana value. If you do, gain control of it."

    spell {
        val t = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Creature))
        effect = Effects.GetEnergy(4) then Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, DynamicAmounts.manaValueOf(t)),
            then = Effects.GainControl(t)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Joseph Meehan"
        flavorText = "\"Citizens, do not resist. This is for your safety. Tezzeret bids you to surrender " +
            "your inventions. Now.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6daf453f-54be-4346-831d-a0434aa086fe.jpg?1783937222"
        ruling(
            "2016-09-20",
            "If the target artifact or creature has a mana value of 0, you may choose to pay zero energy and " +
                "gain control of it."
        )
        ruling("2016-09-20", "If a permanent has {X} in its mana cost, X is considered to be 0.")
        ruling(
            "2016-09-20",
            "You choose the target artifact or creature as you cast Confiscation Coup. You don't choose " +
                "whether or not to pay an amount of {E} until Confiscation Coup resolves. If the target becomes " +
                "an illegal target, Confiscation Coup won't resolve and you won't get {E}{E}{E}{E}."
        )
    }
}

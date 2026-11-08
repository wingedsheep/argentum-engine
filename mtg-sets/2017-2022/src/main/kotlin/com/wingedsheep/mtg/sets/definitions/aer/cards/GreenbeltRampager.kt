package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Greenbelt Rampager
 * {G}
 * Creature — Elephant
 * 3/4
 * When this creature enters, pay {E}{E} (two energy counters). If you can't, return this creature
 * to its owner's hand and you get {E}.
 *
 * The payment is mandatory, not a "may": per the rulings, with {E}{E} or more you must pay, and with
 * fewer you can't pay at all. So it's a resolution-time state test on your energy count — pay exactly
 * two when you have them, otherwise bounce the creature and get one energy.
 */
val GreenbeltRampager = card("Greenbelt Rampager") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elephant"
    power = 3
    toughness = 4
    oracleText =
        "When this creature enters, pay {E}{E} (two energy counters). If you can't, return this " +
        "creature to its owner's hand and you get {E}."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.If(
            condition = Conditions.CompareAmounts(DynamicAmounts.energyCount(), ComparisonOperator.GTE, 2),
            then = Effects.PayExactCounters(CounterType.ENERGY, 2),
            otherwise = Effects.ReturnToHand(EffectTarget.Self) then Effects.GetEnergy(1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "Filip Burburan"
        flavorText = "Subtlety is not a necessary part of sabotage."
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3f06124-bcc4-48c0-a7a6-a461c7485ecd.jpg?1783936744"
        ruling("2017-02-09", "If you have {E}{E}, you have to pay it, even if you don't want to.")
        ruling("2017-02-09", "If you have only one {E}, you can't pay it even if you want to.")
    }
}

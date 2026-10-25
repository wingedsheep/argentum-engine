package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Drizzt Do'Urden — Adventures in the Forgotten Realms #220 (canonical printing)
 * {3}{G}{W} · Legendary Creature — Elf Ranger · 3/3
 *
 * Double strike
 * When Drizzt enters, create Guenhwyvar, a legendary 4/1 green Cat creature token with trample.
 * Whenever a creature dies, if it had power greater than Drizzt's power, put a number of +1/+1
 * counters on Drizzt equal to the difference.
 *
 * The "if" is an intervening-if (CR 603.4): checked when the creature dies and again on
 * resolution (the printed ruling). The dead creature's power is its last-known power, which
 * [DynamicAmounts.triggeringPower] reads off the dies event; Drizzt's is his current projected
 * power, so the difference is computed at resolution — after an earlier trigger has already grown
 * him, a later one may find the gap closed and do nothing.
 */
val DrizztDoUrden = card("Drizzt Do'Urden") {
    manaCost = "{3}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Elf Ranger"
    oracleText = "Double strike\n" +
        "When Drizzt enters, create Guenhwyvar, a legendary 4/1 green Cat creature token with trample.\n" +
        "Whenever a creature dies, if it had power greater than Drizzt's power, put a number of " +
        "+1/+1 counters on Drizzt equal to the difference."
    power = 3
    toughness = 3

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 4,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Cat"),
            keywords = setOf(Keyword.TRAMPLE),
            name = "Guenhwyvar",
            legendary = true,
            imageUri = "https://cards.scryfall.io/normal/front/6/b/6b2c8f52-1580-42d5-8434-c4c70e31e31b.jpg?1783926342"
        )
        description = "When Drizzt enters, create Guenhwyvar, a legendary 4/1 green Cat creature token with trample."
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dies()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.triggeringPower(),
            ComparisonOperator.GT,
            DynamicAmounts.sourcePower()
        )
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.triggeringPower() - DynamicAmounts.sourcePower(),
            EffectTarget.Self
        )
        description = "Whenever a creature dies, if it had power greater than Drizzt's power, put a " +
            "number of +1/+1 counters on Drizzt equal to the difference."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46e3259e-93c2-48c0-8b96-835cdb883383.jpg?1783926449"
        ruling("2021-07-23", "Drizzt's last ability checks his power both at the time the creature dies and at the time the ability resolves. Notably, this means that if several creatures with power greater than Drizzt's die at the same time, Drizzt's ability will trigger that many times, but some of those abilities may not put counters on him as they resolve.")
    }
}

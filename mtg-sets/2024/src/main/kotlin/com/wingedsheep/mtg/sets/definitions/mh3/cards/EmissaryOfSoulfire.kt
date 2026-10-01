package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Emissary of Soulfire
 * {1}{W}{U}
 * Creature — Djinn Monk
 * 1/4
 *
 * When this creature enters, you get {E}{E}{E} (three energy counters).
 * Pay {E}{E}: Put an exalted counter on target creature you control. Activate only as a sorcery.
 *
 * An exalted counter is a keyword counter (CR 122.1b); the engine derives one exalted trigger per
 * counter (see [com.wingedsheep.sdk.scripting.Exalted]), so two counters pump a lone attacker by
 * +2/+2 through two separate triggers.
 */
val EmissaryOfSoulfire = card("Emissary of Soulfire") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Djinn Monk"
    power = 1
    toughness = 4
    oracleText = "When this creature enters, you get {E}{E}{E} (three energy counters).\n" +
        "Pay {E}{E}: Put an exalted counter on target creature you control. Activate only as a " +
        "sorcery. (Whenever a creature you control attacks alone, it gets +1/+1 until end of turn " +
        "for each instance of exalted among permanents you control.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(3)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 2)
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.EXALTED, 1, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "183"
        artist = "Bartek Fedyczak"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/4663e9e3-1989-4ddb-9508-9cc055d2ebe9.jpg?1783911252"

        ruling(
            "2024-06-07",
            "A creature attacks alone if it's the only creature declared as an attacker during the " +
                "declare attackers step (including creatures controlled by your teammates, if " +
                "applicable). For example, exalted won't trigger if you attack with multiple creatures " +
                "and all but one of them are removed from combat. Similarly, creatures that enter the " +
                "battlefield attacking later in combat won't affect exalted abilities that have " +
                "already triggered or resolved."
        )
        ruling("2024-06-07", "A creature with multiple exalted counters will have that many instances of exalted.")
    }
}

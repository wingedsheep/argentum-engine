package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cyclops Superconductor
 * {1}{U}{R}
 * Creature — Cyclops Wizard
 * 2/2
 *
 * Prowess
 * When this creature enters, you get {E}{E}{E} (three energy counters).
 * When this creature dies, you may pay {E}{E}{E}. When you do, this creature deals damage equal
 * to its power to any target.
 *
 * The dies ability is a reflexive trigger (CR 603.12), same shape as Riddle Gate Gargoyle: the
 * "may pay" is untargeted, and the damage chooses its target once the payment is made. The
 * reflexive ability carries the original trigger context, so "its power" reads the last-known
 * power captured when the creature died (prowess pumps included).
 */
val CyclopsSuperconductor = card("Cyclops Superconductor") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Cyclops Wizard"
    power = 2
    toughness = 2
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "When this creature enters, you get {E}{E}{E} (three energy counters).\n" +
        "When this creature dies, you may pay {E}{E}{E}. When you do, this creature deals damage equal " +
        "to its power to any target."

    prowess()

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(3)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayExactCounters(CounterType.ENERGY, 3),
            optional = true,
            descriptionOverride = "You may pay {E}{E}{E}. When you do, this creature deals damage " +
                "equal to its power to any target."
        ) {
            val anyTarget = target(Targets.Any)
            effect = Effects.DealDamage(DynamicAmounts.sourcePower(), anyTarget)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Leonardo Santanna"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69ce6839-92d1-497b-9760-6fdc7388614e.jpg?1783911253"
    }
}

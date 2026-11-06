package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aether Inspector
 * {3}{W}
 * Creature — Dwarf Artificer
 * 2/3
 * Vigilance
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever this creature attacks, you may pay {E}{E}. If you do, create a 1/1 colorless Servo artifact
 * creature token.
 *
 * The attack trigger's "may pay" is an [Effects.MayPay] over an exact energy payment — the prompt is
 * only offered when two energy can actually be paid, and a partial payment is never possible.
 */
val AetherInspector = card("Aether Inspector") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Artificer"
    power = 2
    toughness = 3
    oracleText = "Vigilance\n" +
        "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "Whenever this creature attacks, you may pay {E}{E}. If you do, create a 1/1 colorless Servo " +
        "artifact creature token."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 2),
            then = Effects.CreateToken(
                power = 1,
                toughness = 1,
                creatureTypes = setOf("Servo"),
                artifactToken = true
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Sidharth Chaturvedi"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5dcbe97-d582-464e-98e7-dd06d8652606.jpg?1783936785"
        ruling(
            "2024-06-07",
            "Some triggered abilities state that you \"may pay\" a certain amount of {E}. You can't pay that " +
                "amount multiple times to multiply the effect. You simply choose whether or not to pay that " +
                "amount of {E} as the ability resolves."
        )
    }
}

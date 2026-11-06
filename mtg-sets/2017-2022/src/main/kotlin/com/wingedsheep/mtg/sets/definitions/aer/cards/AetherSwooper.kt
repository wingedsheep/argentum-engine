package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aether Swooper
 * {1}{U}
 * Creature — Vedalken Artificer
 * 1/2
 * Flying
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever this creature attacks, you may pay {E}{E}. If you do, create a 1/1 colorless Servo artifact
 * creature token.
 */
val AetherSwooper = card("Aether Swooper") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Artificer"
    power = 1
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "Whenever this creature attacks, you may pay {E}{E}. If you do, create a 1/1 colorless Servo " +
        "artifact creature token."

    keywords(Keyword.FLYING)

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
        collectorNumber = "26"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c34dbe3-3a66-40b3-a5c2-c2d6acb47773.jpg?1783936776"
        ruling(
            "2024-06-07",
            "Some triggered abilities state that you \"may pay\" a certain amount of {E}. You can't pay that " +
                "amount multiple times to multiply the effect. You simply choose whether or not to pay that " +
                "amount of {E} as the ability resolves."
        )
    }
}

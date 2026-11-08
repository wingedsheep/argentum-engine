package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aether Poisoner
 * {1}{B}
 * Creature — Human Artificer
 * 1/1
 * Deathtouch
 * When this creature enters, you get {E}{E} (two energy counters).
 * Whenever this creature attacks, you may pay {E}{E}. If you do, create a 1/1 colorless Servo artifact
 * creature token.
 */
val AetherPoisoner = card("Aether Poisoner") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Artificer"
    power = 1
    toughness = 1
    oracleText = "Deathtouch\n" +
        "When this creature enters, you get {E}{E} (two energy counters).\n" +
        "Whenever this creature attacks, you may pay {E}{E}. If you do, create a 1/1 colorless Servo " +
        "artifact creature token."

    keywords(Keyword.DEATHTOUCH)

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
        collectorNumber = "51"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9b217f1-1621-40d1-8a98-24c1f7cba800.jpg?1783936766"
        ruling(
            "2024-06-07",
            "Some triggered abilities state that you \"may pay\" a certain amount of {E}. You can't pay that " +
                "amount multiple times to multiply the effect. You simply choose whether or not to pay that " +
                "amount of {E} as the ability resolves."
        )
    }
}

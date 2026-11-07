package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Oviya Pashiri, Sage Lifecrafter
 * {G}
 * Legendary Creature — Human Artificer
 * 1/2
 * {2}{G}, {T}: Create a 1/1 colorless Servo artifact creature token.
 * {4}{G}, {T}: Create an X/X colorless Construct artifact creature token, where X is the number of
 * creatures you control.
 *
 * The Construct's X is the token's `dynamicPower`/`dynamicToughness`, which `CreateTokenExecutor`
 * evaluates once as the ability resolves — before the token exists — and stamps into its base
 * stats. So the token doesn't count itself and keeps its size as the creature count later changes,
 * exactly as the rulings say (the same shape as Metallurgic Summonings' Construct).
 */
val OviyaPashiriSageLifecrafter = card("Oviya Pashiri, Sage Lifecrafter") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Artificer"
    power = 1
    toughness = 2
    oracleText = "{2}{G}, {T}: Create a 1/1 colorless Servo artifact creature token.\n" +
        "{4}{G}, {T}: Create an X/X colorless Construct artifact creature token, where X is the " +
        "number of creatures you control."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true,
        )
        description = "Create a 1/1 colorless Servo artifact creature token."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{G}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            creatureTypes = setOf("Construct"),
            artifactToken = true,
            dynamicPower = DynamicAmounts.creaturesYouControl(),
            dynamicToughness = DynamicAmounts.creaturesYouControl(),
            imageUri = "https://cards.scryfall.io/normal/front/6/2/623a08d1-f5ff-48b7-bdb6-54b8d7a4b931.jpg?1783937240",
        )
        description = "Create an X/X colorless Construct artifact creature token, where X is the " +
            "number of creatures you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Magali Villeneuve"
        flavorText = "\"Inspiration is reciprocal: we all have a responsibility to each other to create.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27eb9d40-958b-41b8-b04b-7f45adc0a862.jpg?1783937175"
        ruling(
            "2016-09-20",
            "The value of X is determined only as Oviya Pashiri's last ability resolves. The token's power " +
                "and toughness won't change as the number of creatures you control changes."
        )
        ruling(
            "2016-09-20",
            "The Construct token you're creating doesn't count towards X for the ability that creates it."
        )
    }
}

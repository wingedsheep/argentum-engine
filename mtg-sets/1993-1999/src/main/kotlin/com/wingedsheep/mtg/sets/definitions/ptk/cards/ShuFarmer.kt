package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Shu Farmer
 * {1}{W}
 * Creature — Human
 * 1/1
 * {T}: You gain 1 life. Activate only during your turn, before attackers are declared.
 */
val ShuFarmer = card("Shu Farmer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human"
    power = 1
    toughness = 1
    oracleText = "{T}: You gain 1 life. Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Li Xiaohua"
        flavorText = "\"The common folk are ceaselessly active. The fields are fertile and the soil productive, and neither flood nor drought plagues us.\"\n—A Shu diplomat"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc6d6524-d8bd-4eb9-9222-747443492b8c.jpg"
    }
}

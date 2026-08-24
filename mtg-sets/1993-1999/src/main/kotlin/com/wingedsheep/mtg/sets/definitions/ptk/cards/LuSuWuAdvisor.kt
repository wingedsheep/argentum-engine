package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Lu Su, Wu Advisor
 * {3}{U}{U}
 * Legendary Creature — Human Advisor
 * 1/2
 * {T}: Draw a card. Activate only during your turn, before attackers are declared.
 *
 * The Portal-era "before attackers are declared" window is two activation restrictions:
 * [ActivationRestriction.OnlyDuringYourTurn] plus [ActivationRestriction.BeforeStep] on the
 * declare-attackers step.
 */
val LuSuWuAdvisor = card("Lu Su, Wu Advisor") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Advisor"
    power = 1
    toughness = 2
    oracleText = "{T}: Draw a card. Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Zhao Tan"
        flavorText = "Lu Su served as an intermediary between the Wu and Shu kingdoms until Zhou Yu's death in 210, when he became Wu's supreme commander."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d361823-31ce-42c4-997d-3d3b52c0599a.jpg"
    }
}

package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Cunning Advisor
 * {3}{B}
 * Creature — Human Advisor
 * 1/1
 * {T}: Target opponent discards a card. Activate only during your turn, before attackers are declared.
 */
val CunningAdvisor = card("Cunning Advisor") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Advisor"
    power = 1
    toughness = 1
    oracleText = "{T}: Target opponent discards a card. Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.discardCards(1, opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Gao Jianzhang"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e31ede4-b0bb-4f63-b8df-1330152611a4.jpg?1783946116"
    }
}

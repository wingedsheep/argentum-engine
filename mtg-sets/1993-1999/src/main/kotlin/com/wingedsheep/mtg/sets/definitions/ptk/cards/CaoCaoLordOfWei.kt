package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Cao Cao, Lord of Wei
 * {3}{B}
 * Legendary Creature — Human Soldier
 * 1/1
 * {T}: Target opponent discards two cards. Activate only during your turn, before attackers are declared.
 */
val CaoCaoLordOfWei = card("Cao Cao, Lord of Wei") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "{T}: Target opponent discards two cards. Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.discardCards(2, opponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Gao Jianzhang"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7e5a530-c954-4696-85ce-6afaf7de1808.jpg?1783946117"
    }
}

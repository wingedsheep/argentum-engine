package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Xun Yu, Wei Advisor
 * {1}{B}{B}
 * Legendary Creature — Human Advisor
 */
val XunYuWeiAdvisor = card("Xun Yu, Wei Advisor") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Advisor"
    power = 1
    toughness = 1
    oracleText = "{T}: Target creature you control gets +2/+0 until end of turn. Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = AbilityCost.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        val creature = target("target", Targets.CreatureYouControl)
        effect = Effects.ModifyStats(2, 0, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "93"
        artist = "Jack Wei"
        flavorText = "\"A splendid talent, admired of all men! His folly lay in serving Cao Cao's power.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f0a25ea-b414-423c-94a6-8a4795d60b46.jpg"
    }
}

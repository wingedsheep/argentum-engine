package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Zhuge Jin, Wu Strategist
 * {1}{U}{U}
 * Legendary Creature — Human Advisor
 * 1/1
 * {T}: Target creature can't be blocked this turn. Activate only during your turn, before attackers
 * are declared.
 */
val ZhugeJinWuStrategist = card("Zhuge Jin, Wu Strategist") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Advisor"
    power = 1
    toughness = 1
    oracleText = "{T}: Target creature can't be blocked this turn. Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        val t = target("target", Targets.Creature)
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Song Shikai"
        flavorText = "When Zhuge Jin proposed the marriage of Guan Yu's daugher and Sun Quan's heir, Guan Yu's arrogant refusal led to disaster."
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60790b07-53da-41fa-b9e0-e7ce22fdcb11.jpg"
    }
}

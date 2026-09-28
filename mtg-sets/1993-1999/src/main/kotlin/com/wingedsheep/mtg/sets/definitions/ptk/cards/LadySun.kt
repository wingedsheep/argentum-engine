package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lady Sun
 * {1}{U}{U}
 * Legendary Creature — Human Advisor
 * 1/1
 *
 * {T}: Return Lady Sun and another target creature to their owners' hands. Activate only
 * during your turn, before attackers are declared.
 */
val LadySun = card("Lady Sun") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Advisor"
    oracleText = "{T}: Return Lady Sun and another target creature to their owners' hands. Activate only during your turn, before attackers are declared."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.ReturnToHand(creature) then Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        flavorText = "Sister to Sun Quan and wife to Liu Bei, Lady Sun often felt her loyalty to both tested."
        collectorNumber = "45"
        artist = "Miao Aili"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f45e837-7b98-46ef-b21b-e3508ce999e5.jpg?1783946122"
    }
}

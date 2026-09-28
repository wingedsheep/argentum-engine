package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Coastal Wizard
 * {2}{U}{U}
 * Creature — Human Wizard
 * 1/1
 *
 * {T}: Return this creature and another target creature to their owners' hands. Activate only
 * during your turn, before attackers are declared.
 */
val CoastalWizard = card("Coastal Wizard") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "{T}: Return this creature and another target creature to their owners' hands. Activate only during your turn, before attackers are declared."
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
        collectorNumber = "34"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b8c7377-1404-44a4-9689-1fc6cdc286c8.jpg"
    }
}

package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kongming's Contraptions
 * {3}{W}
 * Creature — Human Soldier
 * 2/4
 * {T}: This creature deals 2 damage to target attacking creature. Activate only during the declare
 * attackers step and only if you've been attacked this step.
 */
val KongmingsContraptions = card("Kongming's Contraptions") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 4
    oracleText = "{T}: This creature deals 2 damage to target attacking creature. Activate only during the declare attackers step and only if you've been attacked this step."

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.DuringStep(Step.DECLARE_ATTACKERS),
            ActivationRestriction.OnlyIfCondition(YouWereAttackedThisStep)
        )
        val t = target(TargetFilter.Creature.attacking())
        effect = Effects.DealDamage(2, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Jiaming"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6729eb0b-5655-4191-af14-4f7e4a8dded7.jpg?1783946130"
    }
}

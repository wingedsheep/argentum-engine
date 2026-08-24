package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Wu Longbowman
 * {2}{U}
 * Creature — Human Soldier Archer
 * 1/1
 *
 * {T}: This creature deals 1 damage to any target. Activate only during your turn, before
 * attackers are declared.
 */
val WuLongbowman = card("Wu Longbowman") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Soldier Archer"
    power = 1
    toughness = 1
    oracleText = "{T}: This creature deals 1 damage to any target. Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        val t = target("target", Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Xu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28e68635-b0ed-4a56-8b18-679f95db12b6.jpg"
    }
}

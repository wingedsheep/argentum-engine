package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Stone Catapult
 * {4}{B}
 * Creature — Human Soldier
 * 1/2
 * {T}: Destroy target tapped nonblack creature. Activate only during your turn, before attackers are declared.
 */
val StoneCatapult = card("Stone Catapult") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 2
    oracleText = "{T}: Destroy target tapped nonblack creature. Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        val creature = target(TargetFilter.Creature.tapped().notColor(Color.BLACK))
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Shang Huitong"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60303bfe-9158-4e25-a905-6522883ab671.jpg?1783946113"
    }
}

package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hua Tuo, Honored Physician
 * {1}{G}{G}
 * Legendary Creature — Human
 * 1/2
 * {T}: Put target creature card from your graveyard on top of your library. Activate only during your turn, before attackers are declared.
 */
val HuaTuoHonoredPhysician = card("Hua Tuo, Honored Physician") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human"
    power = 1
    toughness = 2
    oracleText = "{T}: Put target creature card from your graveyard on top of your library. Activate only during your turn, before attackers are declared."

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyDuringYourTurn,
            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)
        )
        val t = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.PutOnTopOfLibrary(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "137"
        artist = "Gao Jianzhang"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/bee9c02e-f569-43b5-929a-ec04f661f644.jpg?1783946101"
    }
}

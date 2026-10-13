package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Yavimaya Sojourner
 * {7}{G}
 * Creature — Treefolk
 * 4/6
 * Domain — This spell costs {1} less to cast for each basic land type among lands you control.
 *
 * A self-cast dynamic reduction of the generic part only; the {G} pip is always paid.
 */
val YavimayaSojourner = card("Yavimaya Sojourner") {
    manaCost = "{7}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    power = 4
    toughness = 6
    oracleText = "Domain — This spell costs {1} less to cast for each basic land type among lands you control."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.domain()),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "191"
        artist = "Brian Valeza"
        flavorText = "To the long-lived treefolk, the Phyrexians invading again after only three centuries was as though they never left."
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7fdb424-4d2c-437e-b2a2-8bc56c028338.jpg?1783921290"
    }
}

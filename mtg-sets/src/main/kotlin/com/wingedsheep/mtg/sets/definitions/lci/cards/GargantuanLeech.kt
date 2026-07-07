package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Gargantuan Leech — {7}{B}
 * Creature — Leech
 * 5/5
 *
 * This spell costs {1} less to cast for each Cave you control and each Cave card in your graveyard.
 * Lifelink
 *
 * The cost reduction counts both sources independently: permanents you control with the Cave
 * subtype (battlefield, using projected state) and Cave cards in your graveyard (base state).
 * Two separate [ModifySpellCost] static abilities accumulate additively in [CostCalculator],
 * producing the correct combined reduction — equivalent to the single oracle sentence.
 *
 * LCI #107, Piotr Foksowicz.
 */
val GargantuanLeech = card("Gargantuan Leech") {
    manaCost = "{7}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Leech"
    power = 5
    toughness = 5
    oracleText = "This spell costs {1} less to cast for each Cave you control and each Cave card in your graveyard.\nLifelink"

    // {1} less for each Cave you control
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.PermanentsYouControlMatching(
                    GameObjectFilter.Land.withSubtype("Cave")
                )
            )
        )
    }

    // {1} less for each Cave card in your graveyard
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CardsInGraveyardMatchingFilter(
                    filter = GameObjectFilter.Land.withSubtype("Cave")
                )
            )
        )
    }

    keywords(Keyword.LIFELINK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Piotr Foksowicz"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94724efb-4785-4751-9fe1-07f243dd6008.jpg?1782694526"
    }
}

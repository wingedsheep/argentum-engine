package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Hinata, Dawn-Crowned
 * {1}{U}{R}{W}
 * Legendary Creature — Kirin Spirit
 * 4/4
 * Flying, trample
 * Spells you cast cost {1} less to cast for each target.
 * Spells your opponents cast cost {1} more to cast for each target.
 *
 * Both statics count the spell's *different* targets as it is cast
 * ([CostReductionSource.ChosenTargets]) — a spell aiming two "target creature" phrases at one
 * creature has one target (Hinata's ruling). The reduction is generic-only.
 */
val HinataDawnCrowned = card("Hinata, Dawn-Crowned") {
    manaCost = "{1}{U}{R}{W}"
    colorIdentity = "URW"
    typeLine = "Legendary Creature — Kirin Spirit"
    power = 4
    toughness = 4
    oracleText = "Flying, trample\n" +
        "Spells you cast cost {1} less to cast for each target.\n" +
        "Spells your opponents cast cost {1} more to cast for each target."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any),
            modification = CostModification.ReduceGenericBy(CostReductionSource.ChosenTargets()),
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCast(GameObjectFilter.Any),
            modification = CostModification.IncreaseGenericBy(CostReductionSource.ChosenTargets()),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Alexander Mokhov"
        flavorText = "Hope races on sunlit hooves."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f25aff90-56fd-4f70-bb3b-cabf2900c391.jpg?1783923834"
    }
}

package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deem Inferior
 * {3}{U}
 * Sorcery
 *
 * This spell costs {1} less to cast for each card you've drawn this turn.
 * The owner of target nonland permanent puts it into their library second from the top or on the bottom.
 */
val DeemInferior = card("Deem Inferior") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "This spell costs {1} less to cast for each card you've drawn this turn.\n" +
        "The owner of target nonland permanent puts it into their library second from the top or on the bottom."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.cardsDrawnThisTurn()),
            ),
        )
    }

    spell {
        val t = target(TargetFilter.NonlandPermanent)
        effect = Effects.PutSecondFromTopOrBottomOfLibrary(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Steven Russell Black"
        flavorText = "Forced to behold such a pitiful demonstration of scholarship, Archmage Jeven could hold his tongue no longer."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc0835c7-58ca-4f16-984a-c590ce6a229a.jpg?1783911291"
    }
}

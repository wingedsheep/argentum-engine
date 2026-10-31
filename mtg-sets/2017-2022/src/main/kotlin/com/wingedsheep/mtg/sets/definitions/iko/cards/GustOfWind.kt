package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Gust of Wind
 * {3}{U}
 * Sorcery
 * This spell costs {2} less to cast if you control a creature with flying.
 * Return target nonland permanent you don't control to its owner's hand.
 * Draw a card.
 */
val GustOfWind = card("Gust of Wind") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "This spell costs {2} less to cast if you control a creature with flying.\n" +
        "Return target nonland permanent you don't control to its owner's hand.\n" +
        "Draw a card."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfControlFilter(
                    amount = 2,
                    filter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING)
                )
            )
        )
    }

    spell {
        val t = target(
            TargetFilter(
                GameObjectFilter.NonlandPermanent.withControllerPredicate(
                    ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.ReturnToHand(t) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Adam Paquette"
        flavorText = "Skysail's strength was also its vulnerability."
        imageUri = "https://cards.scryfall.io/normal/front/f/a/faabea92-d8cf-4591-b528-3e597d56b31f.jpg?1783931074"
    }
}

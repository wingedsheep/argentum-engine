package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lullmage's Domination
 * {X}{U}{U}{U}
 * Sorcery
 * This spell costs {3} less to cast if it targets a creature whose controller has eight or more
 * cards in their graveyard.
 * Gain control of target creature with mana value X.
 *
 * "Whose controller has eight or more cards in their graveyard" is `controllerMeets` over
 * `CardsInGraveyardAtLeast(8)`: the condition's "you" is the targeted creature's controller.
 */
val LullmagesDomination = card("Lullmage's Domination") {
    manaCost = "{X}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "This spell costs {3} less to cast if it targets a creature whose controller has eight " +
        "or more cards in their graveyard.\nGain control of target creature with mana value X."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 3,
                    filter = GameObjectFilter.Creature.controllerMeets(Conditions.CardsInGraveyardAtLeast(8)),
                )
            )
        )
    }

    spell {
        val t = target(TargetFilter(GameObjectFilter.Creature.manaValueEqualsX()))
        effect = Effects.GainControl(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Zezhou Chen"
        flavorText = "\"One spell to quiet the Roil, one to quash the will. The principle is the same.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9944393-348f-4f0e-a0c7-9adcc5a30749.jpg?1783929393"
    }
}

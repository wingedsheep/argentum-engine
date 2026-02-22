package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceSpellCostByFilter
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mistform Warchief
 * {2}{U}
 * Creature — Illusion
 * 1/3
 * Creature spells you cast that share a creature type with this creature cost {1} less to cast.
 * {T}: This creature becomes the creature type of your choice until end of turn.
 */
val MistformWarchief = card("Mistform Warchief") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Illusion"
    power = 1
    toughness = 3
    oracleText = "Creature spells you cast that share a creature type with this creature cost {1} less to cast.\n{T}: This creature becomes the creature type of your choice until end of turn."

    staticAbility {
        ability = ReduceSpellCostByFilter(
            filter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.IsCreature,
                    CardPredicate.SharesCreatureTypeWithSource
                )
            ),
            amount = 1
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = BecomeCreatureTypeEffect(
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Greg Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a633d85b-4be1-44a2-8fd8-1ccec4a95ecb.jpg?1562533090"
    }
}

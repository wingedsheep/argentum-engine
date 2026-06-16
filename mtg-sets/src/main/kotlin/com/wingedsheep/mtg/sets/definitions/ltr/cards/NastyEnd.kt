package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Nasty End
 * {1}{B}
 * Instant
 *
 * As an additional cost to cast this spell, sacrifice a creature.
 * Draw two cards. If the sacrificed creature was legendary, draw three cards instead.
 *
 * Implementation: the additional sacrifice cost populates `EffectContext.sacrificedPermanents`
 * with a snapshot whose `supertypes` includes `"LEGENDARY"` when the sacrificed creature was
 * legendary at the moment of payment. The `If`/`Else` branches use that fact directly via the
 * `SacrificedWasLegendary` condition rather than chaining "draw 2, then conditionally draw 1
 * more" — that wording would let a no-op-but-fired draw trigger Underworld Breach-style
 * interactions twice, which is wrong for a single replacement-style choice between "two" and
 * "three" draws.
 */
val NastyEnd = card("Nasty End") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Draw two cards. If the sacrificed creature was legendary, draw three cards instead."

    additionalCost(
        Costs.additional.SacrificePermanent(filter = GameObjectFilter.Creature)
    )

    spell {
        effect = ConditionalEffect(
            condition = Conditions.SacrificedWasLegendary,
            effect = Effects.DrawCards(3),
            elseEffect = Effects.DrawCards(2)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        flavorText = "\"And that's the end of that. A nasty end for Saruman, and I wish I needn't have seen it; but it's a good riddance.\"\n—Sam"
        artist = "Valera Lutfullina"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c34f88d7-15a1-434f-88d7-3d4fcf406d54.jpg?1686968630"
    }
}

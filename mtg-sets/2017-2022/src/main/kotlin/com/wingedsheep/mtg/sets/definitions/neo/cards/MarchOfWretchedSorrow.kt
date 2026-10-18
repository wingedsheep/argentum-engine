package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * March of Wretched Sorrow — Kamigawa: Neon Dynasty #111 (canonical printing)
 * {X}{B} · Instant
 *
 * As an additional cost to cast this spell, you may exile any number of black cards from your hand.
 * This spell costs {2} less to cast for each card exiled this way.
 * March of Wretched Sorrow deals X damage to target creature or planeswalker and you gain X life.
 *
 * The exiled cards are a discount, not the X: each takes {2} off the total cost including the
 * announced X, and both the damage and the life gain read the announced X. The life gain is
 * unconditional (not tied to damage dealt).
 */
val MarchOfWretchedSorrow = card("March of Wretched Sorrow") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may exile any number of black cards " +
        "from your hand. This spell costs {2} less to cast for each card exiled this way.\n" +
        "March of Wretched Sorrow deals X damage to target creature or planeswalker and you gain X life."

    additionalCost(Costs.additional.ExileAnyNumberFromHandForCostReduction(
        GameObjectFilter.Any.withColor(Color.BLACK), reductionPerUnit = 2
    ))

    spell {
        val target = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), target) then
            Effects.GainLife(DynamicAmounts.xValue())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/050a604e-6146-4e2e-88a5-863ecb3dfa1f.jpg?1783923880"
    }
}

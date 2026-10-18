package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * March of Swirling Mist — Kamigawa: Neon Dynasty #61 (canonical printing)
 * {X}{U} · Instant
 *
 * As an additional cost to cast this spell, you may exile any number of blue cards from your hand.
 * This spell costs {2} less to cast for each card exiled this way.
 * Up to X target creatures phase out.
 *
 * The exiled cards are a discount, not the X: each takes {2} off the total cost including the
 * announced X, and the target cap ("up to X") reads the announced X via `dynamicMaxCount`.
 */
val MarchOfSwirlingMist = card("March of Swirling Mist") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may exile any number of blue cards " +
        "from your hand. This spell costs {2} less to cast for each card exiled this way.\n" +
        "Up to X target creatures phase out. (While they're phased out, they're treated as though " +
        "they don't exist. Each one phases in before its controller untaps during their next untap step.)"

    additionalCost(Costs.additional.ExileAnyNumberFromHandForCostReduction(
        GameObjectFilter.Any.withColor(Color.BLUE), reductionPerUnit = 2
    ))

    spell {
        targets(TargetFilter.Creature, optional = true, dynamicMaxCount = DynamicAmounts.xValue())
        effect = Effects.ForEachTarget(Effects.PhaseOut(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Iris Compiet"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/100171d8-7436-44c8-b4cb-0101ffa05c25.jpg?1783923901"
    }
}

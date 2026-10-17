package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * March of Otherworldly Light — Kamigawa: Neon Dynasty #28 (canonical printing)
 * {X}{W} · Instant
 *
 * As an additional cost to cast this spell, you may exile any number of white cards from your hand.
 * This spell costs {2} less to cast for each card exiled this way.
 * Exile target artifact, creature, or enchantment with mana value X or less.
 *
 * The exiled cards are a discount, not the X: each takes {2} off the total cost, which includes the
 * announced X (CR 601.2f), and the target bound reads the announced X.
 */
val MarchOfOtherworldlyLight = card("March of Otherworldly Light") {
    manaCost = "{X}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may exile any number of white cards " +
        "from your hand. This spell costs {2} less to cast for each card exiled this way.\n" +
        "Exile target artifact, creature, or enchantment with mana value X or less."

    additionalCost(Costs.additional.ExileAnyNumberFromHandForCostReduction(
        GameObjectFilter.Any.withColor(Color.WHITE), reductionPerUnit = 2
    ))

    spell {
        val permanent = target(TargetFilter(GameObjectFilter.ArtifactCreatureOrEnchantment.manaValueAtMostX()))
        effect = Effects.Exile(permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Nils Hamm"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/553fb946-2706-475b-89f9-e4355ec9ea2b.jpg?1783923916"
    }
}

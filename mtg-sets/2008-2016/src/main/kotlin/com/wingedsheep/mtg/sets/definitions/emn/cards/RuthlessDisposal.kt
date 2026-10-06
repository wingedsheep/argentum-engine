package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ruthless Disposal — Eldritch Moon #103 (earliest printing; reprinted in Jumpstart 2022)
 * {4}{B}
 * Sorcery
 * As an additional cost to cast this spell, discard a card and sacrifice a creature.
 * Two target creatures each get -13/-13 until end of turn.
 *
 * Two selection additional costs on one spell: both are offered on the cast action and paid
 * together (CR 601.2h).
 */
val RuthlessDisposal = card("Ruthless Disposal") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, discard a card and sacrifice a creature.\n" +
        "Two target creatures each get -13/-13 until end of turn."

    additionalCost(Costs.additional.DiscardCards())
    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        targets(TargetFilter.Creature, count = 2)
        effect = Effects.ForEachTarget(Effects.ModifyStats(-13, -13, EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Dave Kendall"
        flavorText = "\"Do try to pull your weight, Geralf. I feel like I'm doing everything.\"\n—Ghoulcaller Gisa"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe81c78f-e3e2-4513-9711-896e6a1e6aba.jpg?1783937476"
    }
}

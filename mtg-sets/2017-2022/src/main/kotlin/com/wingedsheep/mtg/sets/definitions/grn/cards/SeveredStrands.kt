package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Severed Strands
 * {1}{B}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a creature.
 * You gain life equal to the sacrificed creature's toughness. Destroy target creature an opponent controls.
 */
val SeveredStrands = card("Severed Strands") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "You gain life equal to the sacrificed creature's toughness. Destroy target creature an opponent controls."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        val victim = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.GainLife(DynamicAmounts.sacrificedToughness()) then
            Effects.Destroy(victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Zack Stella"
        flavorText = "\"I've seen Ravnica's fate. Believe me, I'm doing you a favor.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bce654d6-fcf1-40a8-8bdb-5c37e561f7dc.jpg?1783934171"
        ruling("2018-10-05", "You must sacrifice exactly one creature to cast this spell; you can't cast it without sacrificing a creature, and you can't sacrifice additional creatures.")
        ruling("2018-10-05", "The amount of life gained is equal to the sacrificed creature's toughness as it last existed on the battlefield.")
        ruling("2018-10-05", "If the target creature is an illegal target by the time Severed Strands tries to resolve, the spell doesn't resolve. You don't gain life.")
        ruling("2018-10-05", "If the target creature is a legal target but can't be destroyed, most likely because it has indestructible, you still gain life.")
    }
}

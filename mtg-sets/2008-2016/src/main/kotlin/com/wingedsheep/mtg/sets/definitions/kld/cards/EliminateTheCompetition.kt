package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eliminate the Competition — Kaladesh #78 (canonical printing)
 * {4}{B} · Sorcery
 *
 * As an additional cost to cast this spell, sacrifice X creatures.
 * Destroy X target creatures.
 *
 * X isn't in the mana cost — the sacrifice cost defines it. The caster announces X with the spell
 * (CR 601.2b), chooses exactly X targets (601.2c), then sacrifices exactly X creatures as the cost
 * is paid (601.2h). A creature the caster targets may also be one they sacrifice; it is then an
 * illegal target on resolution.
 */
val EliminateTheCompetition = card("Eliminate the Competition") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice X creatures.\nDestroy X target creatures."

    additionalCost(Costs.additional.SacrificeX(GameObjectFilter.Creature))

    spell {
        targets(TargetFilter.Creature, exactly = DynamicAmounts.xValue())
        effect = Effects.ForEachTarget(Effects.Destroy(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Joseph Meehan"
        flavorText = "Open hostilities are strictly forbidden at the Inventors' Fair, but \"accidents\" still happen."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bf09460-2e54-4434-ab03-95eef77265dc.jpg?1783937209"
        ruling("2016-09-20", "Once you announce that you're casting Eliminate the Competition, players can't try to remove creatures you'd like to sacrifice.")
        ruling("2016-09-20", "If you somehow cast Eliminate the Competition without paying its mana cost, you'll still choose a value for X and sacrifice X creatures.")
    }
}

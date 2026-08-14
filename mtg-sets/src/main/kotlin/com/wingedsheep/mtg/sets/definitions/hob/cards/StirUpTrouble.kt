package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stir Up Trouble — The Hobbit #84
 * {B} · Sorcery · Common
 *
 * As an additional cost to cast this spell, sacrifice an artifact or creature or pay {4}.
 * Destroy target creature.
 *
 * The additional cost is the "sacrifice or pay {N}" shape ([Costs.additional.SacrificeOrPay]) — the
 * cast-path enumerator offers both branches and drops the sacrifice branch when you control no
 * artifact or creature, so a player with an empty board can still cast this for {4}{B}.
 */
val StirUpTrouble = card("Stir Up Trouble") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or creature or " +
        "pay {4}.\n" +
        "Destroy target creature."

    additionalCost(
        Costs.additional.SacrificeOrPay(
            filter = GameObjectFilter.CreatureOrArtifact,
            alternativeManaCost = "{4}",
        )
    )

    spell {
        target = Targets.Creature
        effect = Effects.Destroy(EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Andreia Ugrai"
        flavorText = "\"I will prepare something particularly uncomfortable for you, Thorin " +
            "Oakenshield!\"\n—The Great Goblin"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd145e3a-c889-4390-accb-863dbcc845ce.jpg?1785497107"
    }
}

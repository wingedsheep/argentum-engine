package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Launch Party — Return to Ravnica #69
 * {3}{B} · Instant · Common
 *
 * As an additional cost to cast this spell, sacrifice a creature.
 * Destroy target creature. Its controller loses 2 life.
 */
val LaunchParty = card("Launch Party") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\n" +
        "Destroy target creature. Its controller loses 2 life."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then Effects.LoseLife(2, EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Lucas Graciano"
        flavorText = "Life's too short to not do the things you love."
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53f29821-902e-41bc-97a2-6fc7a710cbdb.jpg?1783940361"
    }
}

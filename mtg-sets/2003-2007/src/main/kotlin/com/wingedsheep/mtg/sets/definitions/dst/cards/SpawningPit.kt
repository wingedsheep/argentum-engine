package com.wingedsheep.mtg.sets.definitions.dst.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spawning Pit
 * {2}
 * Artifact
 * Sacrifice a creature: Put a charge counter on this artifact.
 * {1}, Remove two charge counters from this artifact: Create a 2/2 colorless Spawn artifact
 * creature token.
 *
 * Two plain activated abilities: a sacrifice-a-creature cost that banks a charge counter, and a
 * mana-plus-remove-counters cost (the Firemind's Research shape) that cashes two in for a token.
 */
val SpawningPit = card("Spawning Pit") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Sacrifice a creature: Put a charge counter on this artifact.\n" +
        "{1}, Remove two charge counters from this artifact: Create a 2/2 colorless Spawn artifact creature token."

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature)
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
    }
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.RemoveCounterFromSelf(CounterType.CHARGE, 2)
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            creatureTypes = setOf("Spawn"),
            artifactToken = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Tony Szczudlo"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36a3345d-1190-45f4-8191-897b4dcec376.jpg?1783944418"
    }
}

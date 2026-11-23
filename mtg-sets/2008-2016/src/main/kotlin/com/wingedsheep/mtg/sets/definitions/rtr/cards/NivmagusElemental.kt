package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nivmagus Elemental
 * {U/R}
 * Creature — Elemental
 * 1/2
 *
 * Exile an instant or sorcery spell you control: Put two +1/+1 counters on this creature.
 * (That spell won't resolve.)
 *
 * The cost exiles the spell straight off the stack (`Costs.ExileSpellYouControl`): it isn't
 * countered, so it never resolves, and a copy of a spell pays it as well as a cast one.
 */
val NivmagusElemental = card("Nivmagus Elemental") {
    manaCost = "{U/R}"
    colorIdentity = "UR"
    typeLine = "Creature — Elemental"
    power = 1
    toughness = 2
    oracleText = "Exile an instant or sorcery spell you control: Put two +1/+1 counters on this creature. " +
        "(That spell won't resolve.)"

    activatedAbility {
        cost = Costs.ExileSpellYouControl(filter = GameObjectFilter.InstantOrSorcery)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "219"
        artist = "Mike Bierek"
        flavorText = "When it escaped, the experimenters hesitated. It would cause untold havoc, " +
            "yet they wished to see it in action."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1892003-2e4c-43bd-8a37-3a97a76f113a.jpg?1783940327"
    }
}

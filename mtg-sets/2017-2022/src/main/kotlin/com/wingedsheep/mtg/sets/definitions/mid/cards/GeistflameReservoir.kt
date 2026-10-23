package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Geistflame Reservoir
 * {2}{R}
 * Artifact
 * Whenever you cast an instant or sorcery spell, put a charge counter on this artifact.
 * {1}{R}, {T}, Remove any number of charge counters from this artifact: It deals that much damage
 * to any target.
 * {1}{R}, {T}: Exile the top card of your library. You may play that card this turn.
 *
 * "That much" is the ability's X, chosen as it is activated and capped by the charge counters on
 * the Reservoir itself (`RemoveXCounters(self = true)`); X = 0 is a legal activation.
 */
val GeistflameReservoir = card("Geistflame Reservoir") {
    manaCost = "{2}{R}"
    typeLine = "Artifact"
    oracleText = "Whenever you cast an instant or sorcery spell, put a charge counter on this artifact.\n" +
        "{1}{R}, {T}, Remove any number of charge counters from this artifact: It deals that much damage to any target.\n" +
        "{1}{R}, {T}: Exile the top card of your library. You may play that card this turn."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
        description = "Whenever you cast an instant or sorcery spell, put a charge counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R}"),
            Costs.Tap,
            Costs.RemoveXCounters(counterType = CounterType.CHARGE, self = true),
        )
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), anyTarget)
        description = "{1}{R}, {T}, Remove any number of charge counters from this artifact: " +
            "It deals that much damage to any target."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.Tap)
        effect = Patterns.Exile.impulse(1)
        description = "{1}{R}, {T}: Exile the top card of your library. You may play that card this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "142"
        artist = "Anna Christenson"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b247ce7-9c58-404f-bed1-84abb37575ab.jpg?1783925600"
    }
}

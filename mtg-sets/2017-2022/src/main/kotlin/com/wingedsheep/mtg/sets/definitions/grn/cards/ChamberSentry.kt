package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.convergeEntersWithCounters
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chamber Sentry
 * {X}
 * Artifact Creature — Construct
 * 0/0
 * This creature enters with a +1/+1 counter on it for each color of mana spent to cast it.
 * {X}, {T}, Remove X +1/+1 counters from this creature: It deals X damage to any target.
 * {W}{U}{B}{R}{G}: Return this card from your graveyard to your hand.
 *
 * The enters-with clause is the Converge shape ([convergeEntersWithCounters]): colored mana spent on
 * the {X} counts, so a five-color payment of X=5 enters as a 5/5. The damage ability removes counters
 * from the source itself (`self = true`), so X is bounded by its +1/+1 counters (Talon of Pain shape).
 */
val ChamberSentry = card("Chamber Sentry") {
    manaCost = "{X}"
    colorIdentity = "WUBRG"
    typeLine = "Artifact Creature — Construct"
    power = 0
    toughness = 0
    oracleText = "This creature enters with a +1/+1 counter on it for each color of mana spent to cast it.\n" +
        "{X}, {T}, Remove X +1/+1 counters from this creature: It deals X damage to any target.\n" +
        "{W}{U}{B}{R}{G}: Return this card from your graveyard to your hand."

    convergeEntersWithCounters()

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{X}"),
            Costs.Tap,
            Costs.RemoveXCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, self = true),
        )
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), victim)
    }

    activatedAbility {
        cost = Costs.Mana("{W}{U}{B}{R}{G}")
        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15dfd5f2-5298-4cf4-80fe-43db9be24f57.jpg?1783934109"
    }
}

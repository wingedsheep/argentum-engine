package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Filigree Sylex
 * {2}
 * Legendary Artifact
 *
 * {T}: Put an oil counter on The Filigree Sylex.
 * {T}, Sacrifice The Filigree Sylex: Destroy each nonland permanent with mana value equal to the
 * number of oil counters on The Filigree Sylex.
 * {T}, Remove ten oil counters from among permanents you control and sacrifice The Filigree Sylex:
 * It deals 10 damage to any target.
 *
 * The sacrifice is a cost, so the second ability counts the oil counters the Sylex *had* — the
 * snapshot taken as the cost was paid (CR 113.7a), not the zero a graveyard card has. The third
 * ability's ten oil counters may come from any permanents you control, the Sylex itself included,
 * and the damage is dealt by the Sylex as last known on the battlefield.
 */
val TheFiligreeSylex = card("The Filigree Sylex") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "{T}: Put an oil counter on The Filigree Sylex.\n" +
        "{T}, Sacrifice The Filigree Sylex: Destroy each nonland permanent with mana value equal to " +
        "the number of oil counters on The Filigree Sylex.\n" +
        "{T}, Remove ten oil counters from among permanents you control and sacrifice The Filigree " +
        "Sylex: It deals 10 damage to any target."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
        description = "{T}: Put an oil counter on The Filigree Sylex."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.DestroyAll(
            GameObjectFilter.NonlandPermanent.manaValueEqualsDynamic(
                DynamicAmounts.lastKnownSourceCounters(CounterType.OIL)
            )
        )
        description = "{T}, Sacrifice The Filigree Sylex: Destroy each nonland permanent with " +
            "mana value equal to the number of oil counters on The Filigree Sylex."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemoveCounters(count = 10, counterType = CounterType.OIL, filter = GameObjectFilter.Permanent),
            Costs.SacrificeSelf,
        )
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(10, anyTarget, damageSource = EffectTarget.Self)
        description = "{T}, Remove ten oil counters from among permanents you control and " +
            "sacrifice The Filigree Sylex: It deals 10 damage to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "227"
        artist = "Leanna Crossan"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e0958a1-1bac-48be-888d-f7573f409a9b.jpg?1783917992"
        ruling("2023-02-04", "If a permanent has {X} in its mana cost, X is 0 when determining its mana value.")
    }
}

package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Light Up the Night — Innistrad: Midnight Hunt #146.
 * {X}{R} · Sorcery
 *
 * Cast from hand, X comes from the mana cost. The flashback cost {3}{R} has no {X}: the caster
 * announces X with the cast, removes exactly that many loyalty counters from among their
 * planeswalkers ([Costs.additional.RemoveXCounters]), and that X is the spell's X. The flashback's
 * `minimumXValue = 1` is "If you cast this spell this way, X can't be 0". The creature-or-planeswalker
 * test is read against the target as the spell resolves.
 */
val LightUpTheNight = card("Light Up the Night") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Light Up the Night deals X damage to any target. It deals X plus 1 damage instead if that " +
        "target is a creature or planeswalker.\n" +
        "Flashback—{3}{R}, Remove X loyalty counters from among planeswalkers you control. If you cast " +
        "this spell this way, X can't be 0. (You may cast this card from your graveyard for its flashback " +
        "cost. Then exile it.)"

    keywordAbility(
        KeywordAbility.flashback(
            "{3}{R}",
            Costs.additional.RemoveXCounters(CounterType.LOYALTY, GameObjectFilter.Planeswalker),
            minimumXValue = 1,
        )
    )

    spell {
        val t = target(Targets.Any)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.CreatureOrPlaneswalker, t),
            then = Effects.DealDamage(DynamicAmounts.xValue() + 1, t),
            otherwise = Effects.DealDamage(DynamicAmounts.xValue(), t),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Wei Wei"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7de68154-3b82-4a94-98a6-cfc49d359e4e.jpg?1783925598"
        ruling("2025-06-06", "To determine the total cost of a spell, start with the mana cost or alternative cost (such as a flashback cost) you're paying, add any cost increases, then apply any cost reductions. The mana value of the spell is determined only by its mana cost, no matter what the total cost to cast the spell was.")
        ruling("2025-06-06", "A spell cast using flashback will always be exiled afterward, whether it resolves, is countered, or leaves the stack in some other way.")
    }
}

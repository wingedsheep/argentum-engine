package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Twisted Riddlekeeper — Modern Horizons 3 #14 (uncommon)
 * {8} · Creature — Eldrazi Sphinx · 5/5
 *
 * Emerge {5}{C}{U}
 * When you cast this spell, tap up to two target permanents. Put a stun counter on each of them.
 * Flying
 *
 * "Up to two target permanents" is one requirement taking up to two objects (`count = 2,
 * optional = true`), so zero targets is legal. The tap and the stun counter are applied per chosen
 * permanent via [Effects.ForEachTarget]; an already-tapped target still gets its stun counter.
 */
val TwistedRiddlekeeper = card("Twisted Riddlekeeper") {
    manaCost = "{8}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Sphinx"
    power = 5
    toughness = 5
    oracleText = "Emerge {5}{C}{U} (You may cast this spell by sacrificing a creature and paying the " +
        "emerge cost reduced by that creature's mana value.)\n" +
        "When you cast this spell, tap up to two target permanents. Put a stun counter on each of them. " +
        "(If a permanent with a stun counter would become untapped, remove one from it instead.)\n" +
        "Flying"

    emerge("{5}{C}{U}")

    triggeredAbility {
        trigger = Triggers.self.isCast()
        targets(TargetFilter.Permanent, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.AddCounters(CounterType.STUN, 1, EffectTarget.ContextTarget(0))
        )
        description = "When you cast this spell, tap up to two target permanents. Put a stun counter on each of them."
    }

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Nino Vecia"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/534d7ae4-9c4b-4a5a-a109-7630a07aeb45.jpg?1783911306"
        ruling("2024-06-07", "The triggered ability may target permanents that are already tapped. It will still put a stun counter on them.")
        ruling("2024-06-07", "The triggered ability will resolve before Twisted Riddlekeeper does. If Twisted Riddlekeeper is countered or otherwise leaves the stack in response to its triggered ability, the triggered ability will still resolve as normal.")
    }
}

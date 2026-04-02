package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hoarder's Overflow {1}{R}
 * Enchantment
 *
 * When this enchantment enters and whenever you expend 4, put a stash counter on it.
 * (You expend 4 as you spend your fourth total mana to cast spells during a turn.)
 * {1}{R}, Sacrifice this enchantment: Discard your hand, then draw cards equal to the
 * number of stash counters on this enchantment.
 *
 * The sacrifice is modeled as part of the effect (after the draw) rather than the cost,
 * so the engine can read stash counter data before the enchantment leaves the battlefield.
 */
val HoardersOverflow = card("Hoarder's Overflow") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters and whenever you expend 4, put a stash counter on it. (You expend 4 as you spend your fourth total mana to cast spells during a turn.)\n{1}{R}, Sacrifice this enchantment: Discard your hand, then draw cards equal to the number of stash counters on this enchantment."

    // When this enchantment enters, put a stash counter on it.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.AddCounters(Counters.STASH, 1, EffectTarget.Self)
    }

    // Whenever you expend 4, put a stash counter on it.
    triggeredAbility {
        trigger = Triggers.Expend(4)
        effect = Effects.AddCounters(Counters.STASH, 1, EffectTarget.Self)
    }

    // {1}{R}, Sacrifice this enchantment: Discard your hand, then draw cards equal to
    // the number of stash counters on this enchantment.
    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        description = "{1}{R}, Sacrifice: Discard your hand, draw cards equal to stash counters"
        effect = CompositeEffect(
            listOf(
                EffectPatterns.discardHand(),
                DrawCardsEffect(
                    count = DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.STASH)),
                    target = EffectTarget.Controller
                ),
                SacrificeSelfEffect
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Andrea Radeck"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2ed5079-07b4-4575-a2c8-5f0cbff888c3.jpg?1721426652"
    }
}

package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Myth Realized
 * {W}
 * Enchantment
 *
 * Whenever you cast a noncreature spell, put a lore counter on this enchantment.
 * {2}{W}: Put a lore counter on this enchantment.
 * {W}: Until end of turn, this enchantment becomes a Monk Avatar creature in addition to its other
 * types and gains "This creature's power and toughness are each equal to the number of lore
 * counters on it."
 *
 * The granted ability is a characteristic-defining P/T that keeps recomputing, so the lore count
 * goes in `dynamicPower`/`dynamicToughness` (re-evaluated at Layer 7b on every projection) rather
 * than the resolution-frozen `power`/`toughness` pair: a lore counter added after the animation
 * resolves — from the cast trigger or the {2}{W} ability — grows the creature immediately.
 *
 * "In addition to its other types" is [Effects.BecomeCreature]'s default — it stays an
 * Enchantment (and keeps its white colour), only CREATURE and the Monk Avatar subtypes are added.
 * Re-activating it while already animated stamps a newer set-P/T effect, overriding older
 * set-to-a-value effects per the 2015-02-25 ruling.
 */
val MythRealized = card("Myth Realized") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a noncreature spell, put a lore counter on this enchantment.\n" +
        "{2}{W}: Put a lore counter on this enchantment.\n" +
        "{W}: Until end of turn, this enchantment becomes a Monk Avatar creature in addition to its " +
        "other types and gains \"This creature's power and toughness are each equal to the number " +
        "of lore counters on it.\""

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.LORE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        effect = Effects.AddCounters(CounterType.LORE, 1, EffectTarget.Self)
        description = "{2}{W}: Put a lore counter on this enchantment."
    }

    activatedAbility {
        cost = Costs.Mana("{W}")
        val lore = DynamicAmounts.countersOnSelf(CounterType.LORE)
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = DynamicAmounts.fixed(0),
            toughness = DynamicAmounts.fixed(0),
            creatureTypes = setOf("Monk", "Avatar"),
            duration = Duration.EndOfTurn,
            dynamicPower = lore,
            dynamicToughness = lore,
        )
        description = "{W}: Until end of turn, this enchantment becomes a Monk Avatar creature with " +
            "power and toughness each equal to the number of lore counters on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37bd9f67-32a7-4022-bb53-5bfc671520c7.jpg?1783938617"
        ruling("2015-02-25", "Myth Realized can't attack the turn it enters the battlefield.")
        ruling(
            "2015-02-25",
            "Activating the ability that turns Myth Realized into a creature while it's already a " +
                "creature will override any effects that set its power or toughness to a specific " +
                "value. Effects that modify power or toughness without directly setting them to a " +
                "specific value will continue to apply."
        )
    }
}

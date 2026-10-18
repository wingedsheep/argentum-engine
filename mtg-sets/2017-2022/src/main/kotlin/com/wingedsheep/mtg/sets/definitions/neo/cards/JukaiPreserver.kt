package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jukai Preserver — Kamigawa: Neon Dynasty #195
 * {3}{G} · Enchantment Creature — Human Druid · 3/3
 *
 * When this creature enters, put a +1/+1 counter on target creature you control.
 * Channel — {2}{G}, Discard this card: Put a +1/+1 counter on each of up to two target creatures
 * you control.
 *
 * The Channel half is Zimone's "each of up to two targets" shape: an optional two-count target
 * requirement fanned out by [Effects.ForEachTarget], activated from hand with a discard-self cost.
 */
val JukaiPreserver = card("Jukai Preserver") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Human Druid"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, put a +1/+1 counter on target creature you control.\n" +
        "Channel — {2}{G}, Discard this card: Put a +1/+1 counter on each of up to two target " +
        "creatures you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
        description = "When this creature enters, put a +1/+1 counter on target creature you control."
    }

    // Channel — {2}{G}, Discard this card (from hand): counters on up to two of your creatures.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        targets(TargetFilter.Creature.youControl(), count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
        description = "Channel — {2}{G}, Discard this card: Put a +1/+1 counter on each of up to two " +
            "target creatures you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Donato Giancola"
        flavorText = "\"The kami grant you this boon, not I.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e66120a5-95a3-4d15-873c-cfba221a2299.jpg?1783923846"
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
    }
}

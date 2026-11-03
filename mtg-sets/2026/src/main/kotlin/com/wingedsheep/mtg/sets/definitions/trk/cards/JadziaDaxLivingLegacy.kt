package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jadzia Dax, Living Legacy
 * {1}{G}
 * Legendary Creature — Trill Scientist
 * 0/1
 *
 * At the beginning of your first main phase, put a +1/+1 counter on Jadzia Dax, then add an amount
 * of {G} equal to the number of counters on her.
 * Transfer Symbiont — {2}{G}, Discard this card: Put two +1/+1 counters on target creature.
 *
 * The mana trigger is Black Market's shape (`beginningOf(Step.PRECOMBAT_MAIN)` is the first main
 * phase), preceded by the counter. "The number of counters" counts every kind, so the amount reads
 * `countersOnSelf(null)` after the new counter lands. Transfer Symbiont is a channel-style ability
 * activated from hand with the discard as part of its cost.
 */
val JadziaDaxLivingLegacy = card("Jadzia Dax, Living Legacy") {
    manaCost = "{1}{G}"
    typeLine = "Legendary Creature — Trill Scientist"
    colorIdentity = "G"
    power = 0
    toughness = 1
    oracleText = "At the beginning of your first main phase, put a +1/+1 counter on Jadzia Dax, then add an " +
        "amount of {G} equal to the number of counters on her.\n" +
        "Transfer Symbiont — {2}{G}, Discard this card: Put two +1/+1 counters on target creature."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.AddMana(Color.GREEN, DynamicAmounts.countersOnSelf(null))
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
        description = "Transfer Symbiont — {2}{G}, Discard this card: Put two +1/+1 counters on target creature."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "204"
        artist = "John Tyler Christopher"
        flavorText = "\"If you want to know who you are, it's important to know who you've been.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd342f8b-9072-487d-8a33-b1bb149f1502.jpg?1793013267"
    }
}

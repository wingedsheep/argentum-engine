package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cyclone
 * {2}{G}{G}
 * Enchantment
 * At the beginning of your upkeep, put a wind counter on this enchantment, then sacrifice this
 * enchantment unless you pay {G} for each wind counter on it. If you pay, this enchantment deals
 * damage equal to the number of wind counters on it to each creature and each player.
 *
 * Composition:
 *  - Each upkeep adds a wind counter (passive [Counters.WIND]), then the pay-or-sacrifice is an
 *    `OptionalCostEffect` (Gate.MayPay): pay {G} per wind counter (a colored dynamic mana cost via
 *    `Effects.PayDynamicMana(..., color = GREEN)`) → deal damage; decline → sacrifice.
 *  - Both the cost amount and the damage scale off `DynamicAmounts.countersOnSelf(WIND)`, evaluated
 *    after the counter is added so they see the incremented total (CR 608.2c sequencing).
 *  - The damage hits each creature (`ForEachInGroup(AllCreatures, …)`) and each player.
 */
val Cyclone = card("Cyclone") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, put a wind counter on this enchantment, then " +
        "sacrifice this enchantment unless you pay {G} for each wind counter on it. If you pay, " +
        "this enchantment deals damage equal to the number of wind counters on it to each creature " +
        "and each player."

    triggeredAbility {
        trigger = Triggers.YourUpkeep

        val windCount = DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.WIND))

        val dealDamageToAll = Effects.Composite(
            Effects.ForEachInGroup(
                GroupFilter.AllCreatures,
                DealDamageEffect(windCount, EffectTarget.Self)
            ),
            Effects.DealDamage(windCount, EffectTarget.PlayerRef(Player.Each))
        )

        effect = Effects.Composite(
            Effects.AddCounters(Counters.WIND, 1, EffectTarget.Self),
            OptionalCostEffect(
                cost = Effects.PayDynamicMana(windCount, color = Color.GREEN),
                ifPaid = dealDamageToAll,
                ifNotPaid = Effects.SacrificeTarget(EffectTarget.Self)
            )
        )
        description = "At the beginning of your upkeep, put a wind counter on this enchantment, then " +
            "sacrifice this enchantment unless you pay {G} for each wind counter on it. If you pay, " +
            "this enchantment deals damage equal to the number of wind counters on it to each creature and each player."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f11684d6-5b74-47a7-a2d0-256c9e437aa6.jpg?1562940349"
    }
}

package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Peema Aether-Seer
 * {3}{G}
 * Creature — Elf Druid
 * 3/2
 * When this creature enters, you get an amount of {E} (energy counters) equal to the greatest power
 * among creatures you control.
 * Pay {E}{E}{E}: Target creature blocks this turn if able.
 *
 * - The greatest power is read on resolution and includes Peema itself (it's on the battlefield).
 * - "Blocks this turn if able" is [Effects.MarkMustBlockThisTurn]: a requirement, not a guarantee.
 */
val PeemaAetherSeer = card("Peema Aether-Seer") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, you get an amount of {E} (energy counters) equal to the " +
        "greatest power among creatures you control.\n" +
        "Pay {E}{E}{E}: Target creature blocks this turn if able."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddDynamicCounters(
            CounterType.ENERGY,
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower(),
            EffectTarget.Controller
        )
        description = "When this creature enters, you get an amount of {E} equal to the greatest power " +
            "among creatures you control."
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 3)
        val creature = target(TargetFilter.Creature)
        effect = Effects.MarkMustBlockThisTurn(creature)
        description = "Pay {E}{E}{E}: Target creature blocks this turn if able."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Howard Lyon"
        flavorText = "\"The Great Conduit connects us all.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe1bc709-2157-4711-a027-c0ff00ff2728.jpg?1783936741"
        ruling(
            "2017-02-09",
            "The target creature blocks only if it's able to do so as the declare blockers step begins. If, at " +
                "that time, the creature is tapped, it's affected by a spell or ability that says it can't block, " +
                "or no creatures are attacking its controller or a planeswalker controlled by that player, then it " +
                "doesn't block. If there's a cost associated with having the creature block, the player isn't " +
                "forced to pay that cost. If that cost isn't paid, the creature won't block."
        )
        ruling(
            "2017-02-09",
            "The controller of the target creature chooses which attacking creature that creature blocks."
        )
        ruling(
            "2017-02-09",
            "The amount of {E} you get is determined as Peema Aether-Seer's first ability resolves. If the " +
                "greatest power among creatures you control is somehow negative, you don't get or lose any {E}."
        )
    }
}

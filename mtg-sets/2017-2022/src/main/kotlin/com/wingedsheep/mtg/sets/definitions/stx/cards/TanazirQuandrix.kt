package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tanazir Quandrix — Strixhaven: School of Mages #240 (canonical printing)
 * {3}{G}{U} · Legendary Creature — Elder Dragon · 4/4
 *
 * Flying, trample
 * When Tanazir Quandrix enters, double the number of +1/+1 counters on target creature you control.
 * Whenever Tanazir Quandrix attacks, you may have the base power and toughness of other creatures
 * you control become equal to Tanazir Quandrix's power and toughness until end of turn.
 *
 * - The ETB is [Effects.DoubleCounters] on one target (Dragonsguard Elite's doubling: put as many
 *   +1/+1 counters as it already has, ruling).
 * - The attack trigger is an optional [Effects.ForEachInGroup] over the other creatures you control
 *   setting base P/T (layer 7b, Mirror Entity's shape) to Tanazir's *actual* power and toughness
 *   ([DynamicAmounts.sourcePower] / [DynamicAmounts.sourceToughness], projected, with last-known
 *   information if Tanazir has left). The amounts are locked in as the ability resolves — not
 *   reevaluated — so a later change to Tanazir's P/T doesn't move them (ruling).
 */
val TanazirQuandrix = card("Tanazir Quandrix") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying, trample\n" +
        "When Tanazir Quandrix enters, double the number of +1/+1 counters on target creature you control.\n" +
        "Whenever Tanazir Quandrix attacks, you may have the base power and toughness of other " +
        "creatures you control become equal to Tanazir Quandrix's power and toughness until end of turn."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.DoubleCounters(CounterType.PLUS_ONE_PLUS_ONE, creature)
        description = "When Tanazir Quandrix enters, double the number of +1/+1 counters on " +
            "target creature you control."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        optional = true
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.OtherCreaturesYouControl,
            effect = Effects.SetBasePowerAndToughness(
                power = DynamicAmounts.sourcePower(),
                toughness = DynamicAmounts.sourceToughness(),
                target = EffectTarget.IterationEntity,
                duration = Duration.EndOfTurn,
            )
        )
        description = "Whenever Tanazir Quandrix attacks, you may have the base power and " +
            "toughness of other creatures you control become equal to Tanazir Quandrix's power " +
            "and toughness until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "240"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bd62ee4-e654-44ba-a650-889782cc9ac3.jpg?1783927287"
        ruling("2021-04-16", "To double the number of +1/+1 counters on a permanent, put a number of +1/+1 counters on it equal to the number it already has. Other cards that interact with putting counters on it will interact with this effect accordingly.")
        ruling("2021-04-16", "As the last ability resolves, the base power and toughness of other creatures you control are set to Tanazir Quandrix's actual power and toughness, not just its base power and toughness. If Tanazir Quandrix's power or toughness changes later in the turn, the other creatures you control aren't affected.")
        ruling("2021-04-16", "Any effects that modify a creature's power and/or toughness without setting them to a specific value (i.e. ones that don't affect base power and/or toughness) will apply after its base power and toughness are set, regardless of the order those effects were created. The same is true for counters that modify its power and toughness.")
        ruling("2021-04-16", "Tanazir Quandrix's last ability overwrites all previous effects that set a creature's power and toughness to specific values. Other effects that set its power or toughness to specific values that start to apply after the ability resolves will overwrite this effect.")
    }
}

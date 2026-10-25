package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Targ Nar, Demon-Fang Gnoll
 * {R}{G}
 * Legendary Creature — Gnoll
 * 2/2
 * Pack tactics — Whenever Targ Nar attacks, if you attacked with creatures with total power 6 or
 * greater this combat, attacking creatures get +1/+0 until end of turn.
 * {2}{R}{G}: Double Targ Nar's power and toughness until end of turn.
 *
 * The pump is a one-shot over the attacking creatures as the trigger resolves (it does not reach
 * creatures that start attacking later). "Double" is +X/+Y where X and Y are its power and toughness
 * as the effect begins to apply (the Reckless Amplimancer shape).
 */
val TargNarDemonFangGnoll = card("Targ Nar, Demon-Fang Gnoll") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Gnoll"
    oracleText = "Pack tactics — Whenever Targ Nar attacks, if you attacked with creatures with total power 6 " +
        "or greater this combat, attacking creatures get +1/+0 until end of turn.\n" +
        "{2}{R}{G}: Double Targ Nar's power and toughness until end of turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackersTotalPowerAtLeast(6)))
        effect = Effects.ForEachInGroup(
            GroupFilter.AttackingCreatures,
            Effects.ModifyStats(1, 0, EffectTarget.IterationEntity)
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{R}{G}")
        effect = Effects.ModifyStats(DynamicAmounts.sourcePower(), DynamicAmounts.sourceToughness(), EffectTarget.Self)
        description = "{2}{R}{G}: Double Targ Nar's power and toughness until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fe57856-9d35-470e-9138-360b02ac0c90.jpg?1783926443"
        ruling("2021-07-23", "If an effect instructs you to \"double\" a creature's power, that creature gets +X/+0, where X is its power as that effect begins to apply. The same is true for toughness.")
        ruling("2021-07-23", "If a creature's power is less than 0 when it's doubled, instead that creature gets -X/-0, where X is how much less than 0 its power is. For example, if an effect has given a 2/2 creature -4/-0 so that it's a -2/2 creature, doubling its power and toughness gives it -2/+2, and it becomes a -4/4 creature.")
    }
}

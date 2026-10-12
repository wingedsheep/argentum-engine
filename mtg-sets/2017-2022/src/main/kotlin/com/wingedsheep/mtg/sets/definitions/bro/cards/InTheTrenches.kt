package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * In the Trenches — The Brothers' War #8
 * {1}{W}{W} · Enchantment
 *
 * Creatures you control get +1/+1.
 * {5}{W}: Exile target nonland permanent you don't control until this enchantment leaves the
 * battlefield. Activate only as a sorcery and only once.
 *
 * "Until this enchantment leaves the battlefield" is [Effects.MoveUntilSourceLeaves]: the return is
 * a one-shot effect, not a leaves trigger, and nothing is exiled if In the Trenches is already gone
 * when the ability resolves. "You don't control" is `Not(ControlledByYou)` rather than
 * `opponentControls()` (they differ for a teammate's permanents). "Only once" is
 * [ActivationRestriction.Once] — once for this object's lifetime, so a countered activation still
 * spends it, and a returned In the Trenches is a new object that may activate again.
 */
val InTheTrenches = card("In the Trenches") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Creatures you control get +1/+1.\n" +
        "{5}{W}: Exile target nonland permanent you don't control until this enchantment leaves the " +
        "battlefield. Activate only as a sorcery and only once."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    activatedAbility {
        cost = Costs.Mana("{5}{W}")
        val victim = target(
            TargetFilter(
                GameObjectFilter.NonlandPermanent.withControllerPredicate(
                    ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        effect = Effects.MoveUntilSourceLeaves(victim, Zone.EXILE)
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(ActivationRestriction.Once)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "8"
        artist = "Daarken"
        flavorText = "The battlefield became so pockmarked with craters and trenches that casualties " +
            "were regularly attributed to \"fell in a hole.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71ff86e9-1ce6-43cc-8135-f2adb1b6c5a7.jpg?1783920131"
        ruling("2022-10-14", "If In the Trenches leaves the battlefield before its activated ability resolves, the target permanent won't be exiled.")
        ruling("2022-10-14", "If a token is exiled, it ceases to exist. It won't be returned to the battlefield.")
        ruling("2022-10-14", "The activated ability can only be activated once per game. If the ability is countered or otherwise fails to resolve after being activated, it can't be activated again.")
        ruling("2022-10-14", "If In the Trenches leaves the battlefield and later returns to the battlefield, it is a new object on the battlefield with no memory of the last time it was there. Its ability may be activated, even if it was activated a previous time it was on the battlefield.")
    }
}

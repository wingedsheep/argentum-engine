package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Add Echo [cost] (CR 702.30) — the display keyword plus the triggered ability it abbreviates:
 *
 * > **CR 702.30a** — "Echo [cost]" means "At the beginning of your upkeep, if this permanent came
 * > under your control since the beginning of your last upkeep, sacrifice it unless you pay [cost]."
 *
 * Pure composition:
 *  - `Triggers.you.beginningOf(UPKEEP)` with the intervening `if` "on the battlefield and *not*
 *    [GameObjectFilter.controlledSinceYourLastUpkeep]" — so it asks on the first upkeep after it
 *    entered or changed control, never again while you keep controlling it, and again after it
 *    leaves your control and comes back (a new controller, or a blink, owes the echo anew);
 *  - [Effects.MayPay] of [cost] whose `otherwise` sacrifices it. An unaffordable cost skips the
 *    prompt straight to the sacrifice.
 *
 * **Scope: mana echo costs** (Mogg War Marshal's `{1}{R}`, the Urza-block "echo equal to its mana
 * cost"). Time Spiral's non-mana echo costs ("Echo—Discard a card") are not covered yet.
 */
fun CardBuilder.echo(cost: ManaCost) {
    keywordSet.add(Keyword.ECHO)
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.All(
            Conditions.SourceInZone(Zone.BATTLEFIELD),
            Conditions.Not(Conditions.SourceMatches(GameObjectFilter.Any.controlledSinceYourLastUpkeep())),
        )
        effect = Effects.MayPay(
            cost = Effects.PayMana(cost.toString()),
            then = Effects.Nothing,
            otherwise = Effects.SacrificeTarget(EffectTarget.Self),
            descriptionOverride = "Pay echo $cost, or sacrifice this permanent?",
        )
        description = "Echo $cost (At the beginning of your upkeep, if this came under your control since " +
            "the beginning of your last upkeep, sacrifice it unless you pay its echo cost.)"
    }
}

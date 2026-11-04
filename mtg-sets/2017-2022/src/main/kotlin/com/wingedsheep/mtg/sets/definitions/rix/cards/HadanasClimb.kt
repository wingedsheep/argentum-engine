package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hadana's Climb // Winged Temple of Orazca (Rivals of Ixalan — the card's earliest printing)
 * {1}{G}{U}
 * Legendary Enchantment // Legendary Land
 *
 * Front — Hadana's Climb (Legendary Enchantment, {1}{G}{U})
 *   At the beginning of combat on your turn, put a +1/+1 counter on target creature you control.
 *   Then if that creature has three or more +1/+1 counters on it, transform Hadana's Climb.
 *
 * Back — Winged Temple of Orazca (Legendary Land)
 *   {T}: Add one mana of any color.
 *   {1}{G}{U}, {T}: Target creature you control gains flying and gets +X/+X until end of turn,
 *   where X is its power.
 *
 * Implementation:
 *  - Combat trigger: [Effects.AddCounters] on the target, then an [Effects.If] comparing
 *    [DynamicAmounts.countersOn] the same target against 3 (Ordeal of Nylea's shape) wrapping a
 *    mandatory [Effects.Transform]. The count is read on resolution, after the counter lands; an
 *    illegal target fizzles the whole ability, so the Climb doesn't transform.
 *  - Back's pump reads X once on resolution off the target's power ([DynamicAmounts.powerOf],
 *    Berserk's shape), so later pumps don't re-scale it.
 */

private val HadanasClimbFront = card("Hadana's Climb") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Enchantment"
    oracleText = "At the beginning of combat on your turn, put a +1/+1 counter on target " +
        "creature you control. Then if that creature has three or more +1/+1 counters on it, " +
        "transform Hadana's Climb."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    left = DynamicAmounts.countersOn(creature, CounterType.PLUS_ONE_PLUS_ONE),
                    operator = ComparisonOperator.GTE,
                    right = 3,
                ),
                then = Effects.Transform(EffectTarget.Self),
            )
        description = "At the beginning of combat on your turn, put a +1/+1 counter on target " +
            "creature you control. Then if that creature has three or more +1/+1 counters on " +
            "it, transform Hadana's Climb."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Titus Lunter"
        flavorText = "Hadana seeks power in the service of peace."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e7554bc-8583-4059-8895-c3845bc27ae3.jpg?1783935281"
    }
}

private val WingedTempleOfOrazca = card("Winged Temple of Orazca") {
    manaCost = ""
    colorIdentity = "GU"
    typeLine = "Legendary Land"
    oracleText = "(Transforms from Hadana's Climb.)\n" +
        "{T}: Add one mana of any color.\n" +
        "{1}{G}{U}, {T}: Target creature you control gains flying and gets +X/+X until end of " +
        "turn, where X is its power."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}{U}"), Costs.Tap)
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.FLYING, creature) then
            Effects.ModifyStats(DynamicAmounts.powerOf(creature), DynamicAmounts.powerOf(creature), creature)
        description = "{1}{G}{U}, {T}: Target creature you control gains flying and gets +X/+X " +
            "until end of turn, where X is its power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/back/8/e/8e7554bc-8583-4059-8895-c3845bc27ae3.jpg?1783935281"
    }
}

val HadanasClimb: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = HadanasClimbFront,
    backFace = WingedTempleOfOrazca,
)

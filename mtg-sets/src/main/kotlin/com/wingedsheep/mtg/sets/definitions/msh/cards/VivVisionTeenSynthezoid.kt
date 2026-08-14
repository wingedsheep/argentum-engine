package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Viv Vision, Teen Synthezoid — Marvel Super Heroes #256 (uncommon)
 * {3} · Legendary Artifact Creature — Robot Hero · 2/2
 *
 * Flying
 * Cybernetic Senses — Whenever Viv Vision attacks, draw a card if her power is 4 or greater.
 * Power-up — {7}: Put two +1/+1 counters on Viv Vision. (Activate each power-up ability only
 * once. Reduce the cost by her mana cost if she entered this turn.)
 *
 * The power check is **not** an intervening-if: the printed wording is "draw a card *if* her
 * power is 4 or greater", with the condition trailing the effect rather than sitting between the
 * trigger and it. So the trigger always goes on the stack when she attacks, and the condition is
 * read once at resolution — a [ConditionalEffect], not a `triggerCondition`. The distinction is
 * live on this card: pump her in response to the trigger and you still draw.
 *
 * Her power is read through [DynamicAmounts.sourcePower], which sees projected power, so counters
 * and any lord effects both count toward the 4.
 *
 * `{7}` − `{3}` = `{4}`, which is exactly enough to turn her into the 4/4 the trigger wants on the
 * turn she lands.
 */
val VivVisionTeenSynthezoid = card("Viv Vision, Teen Synthezoid") {
    manaCost = "{3}"
    typeLine = "Legendary Artifact Creature — Robot Hero"
    oracleText = "Flying\n" +
        "Cybernetic Senses — Whenever Viv Vision attacks, draw a card if her power is 4 or greater.\n" +
        "Power-up — {7}: Put two +1/+1 counters on Viv Vision. (Activate each power-up ability " +
        "only once. Reduce the cost by her mana cost if she entered this turn.)"
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = ConditionalEffect(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.sourcePower(),
                ComparisonOperator.GTE,
                DynamicAmount.Fixed(4)
            ),
            effect = Effects.DrawCards(1)
        )
        description = "Cybernetic Senses — Whenever Viv Vision attacks, draw a card if her power " +
            "is 4 or greater."
    }

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{7}")
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "256"
        artist = "Nereida"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85cc170a-ecd2-4870-b675-7ece88813995.jpg?1783902886"
    }
}

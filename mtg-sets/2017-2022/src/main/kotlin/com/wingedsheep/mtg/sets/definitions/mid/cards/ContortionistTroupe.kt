package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Contortionist Troupe (Innistrad: Midnight Hunt #178)
 * {X}{G}
 * Creature — Human
 * 0/0
 * This creature enters with X +1/+1 counters on it.
 * Coven — At the beginning of your end step, if you control three or more creatures with different
 * powers, put a +1/+1 counter on target creature you control.
 *
 * Coven is an intervening-if (checked when the trigger fires and again on resolution), counted as
 * distinct power values among creatures you control.
 */
val ContortionistTroupe = card("Contortionist Troupe") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human"
    oracleText = "This creature enters with X +1/+1 counters on it.\n" +
        "Coven — At the beginning of your end step, if you control three or more creatures with " +
        "different powers, put a +1/+1 counter on target creature you control."
    power = 0
    toughness = 0

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            left = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature,
            ).distinctValues(CardNumericProperty.POWER),
            operator = ComparisonOperator.GTE,
            right = 3,
        )
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Jesper Ejsing"
        flavorText = "\"Welcome to the greatest show in Kessig!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c81da535-04e4-4dc8-9e12-7b417aba2f39.jpg?1783925581"
        ruling("2021-09-24", "Many coven abilities, such as that of Dawnhart Wardens above, are triggered abilities with intervening if clauses. You must control three or more creatures with different powers at the time the ability triggers and at the time the ability tries to resolve. They do not, however, need to be the same set of creatures in both cases.")
        ruling("2021-09-24", "A creature has different power from another if their powers are different numbers. For example, a 1/1 creature and a 2/1 creature have different powers.")
        ruling("2021-09-24", "For three creatures to have different powers from one another, each of their powers needs to be different. A 1/1 creature, a 2/1 creature, and another 2/1 creature aren't three creatures with different powers, even though both 2/1 creatures have different power than the 1/1 creature.")
    }
}

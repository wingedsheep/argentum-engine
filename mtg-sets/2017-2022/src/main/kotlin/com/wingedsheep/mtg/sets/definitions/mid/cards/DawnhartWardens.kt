package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Dawnhart Wardens (Innistrad: Midnight Hunt #216)
 * {1}{G}{W}
 * Creature — Human Warlock
 * 3/3
 * Vigilance
 * Coven — At the beginning of combat on your turn, if you control three or more creatures with
 * different powers, creatures you control get +1/+0 until end of turn.
 */
val DawnhartWardens = card("Dawnhart Wardens") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Human Warlock"
    oracleText = "Vigilance\nCoven — At the beginning of combat on your turn, if you control three or more " +
        "creatures with different powers, creatures you control get +1/+0 until end of turn."
    power = 3
    toughness = 3

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.CompareAmounts(
            left = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature,
            ).distinctValues(CardNumericProperty.POWER),
            operator = ComparisonOperator.GTE,
            right = 3,
        )
        effect = Patterns.Group.modifyStatsForAll(
            1, 0,
            GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Joshua Raphael"
        flavorText = "\"The balance of day and night is shattered. We must find the Moonsilver Key.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7eb2f0a7-f3a6-4487-b847-4ef26ad5ecad.jpg?1783925565"
        ruling("2021-09-24", "For three creatures to have different powers from one another, each of their powers needs to be different. A 1/1 creature, a 2/1 creature, and another 2/1 creature aren't three creatures with different powers, even though both 2/1 creatures have different power than the 1/1 creature.")
        ruling("2021-09-24", "A creature has different power from another if their powers are different numbers. For example, a 1/1 creature and a 2/1 creature have different powers.")
        ruling("2021-09-24", "Many coven abilities, such as that of Dawnhart Wardens above, are triggered abilities with intervening if clauses. You must control three or more creatures with different powers at the time the ability triggers and at the time the ability tries to resolve. They do not, however, need to be the same set of creatures in both cases.")
    }
}

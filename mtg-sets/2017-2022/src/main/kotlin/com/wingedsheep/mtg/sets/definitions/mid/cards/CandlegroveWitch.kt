package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Candlegrove Witch (Innistrad: Midnight Hunt #8)
 * {1}{W}
 * Creature — Human Warlock
 * 2/2
 * Coven — At the beginning of combat on your turn, if you control three or more creatures with
 * different powers, this creature gains flying until end of turn.
 *
 * Coven is an intervening-if (checked when the trigger fires and again on resolution): three or more
 * creatures you control with pairwise-different powers, counted as distinct power values.
 */
val CandlegroveWitch = card("Candlegrove Witch") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warlock"
    oracleText = "Coven — At the beginning of combat on your turn, if you control three or more " +
        "creatures with different powers, this creature gains flying until end of turn."
    power = 2
    toughness = 2

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
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Anna Christenson"
        flavorText = "\"Like a thousand candles fighting back the darkness, we are strongest when we stand together.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/e/dedfe6ea-cd43-4ec4-83a1-1f0f74e27f56.jpg?1783925665"
        ruling("2021-09-24", "For three creatures to have different powers from one another, each of their powers needs to be different. A 1/1 creature, a 2/1 creature, and another 2/1 creature aren't three creatures with different powers, even though both 2/1 creatures have different power than the 1/1 creature.")
        ruling("2021-09-24", "You must control three or more creatures with different powers at the time the ability triggers and at the time the ability tries to resolve. They do not, however, need to be the same set of creatures in both cases.")
    }
}

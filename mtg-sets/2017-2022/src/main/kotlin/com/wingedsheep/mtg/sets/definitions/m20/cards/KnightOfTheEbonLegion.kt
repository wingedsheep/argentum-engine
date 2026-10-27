package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Knight of the Ebon Legion
 * {B}
 * Creature — Vampire Knight
 * 1/2
 * {2}{B}: This creature gets +3/+3 and gains deathtouch until end of turn.
 * At the beginning of your end step, if a player lost 4 or more life this turn, put a +1/+1
 * counter on this creature.
 *
 * "A player lost 4 or more life" is a per-player threshold over the whole turn's cumulative life
 * loss (rulings: losses add up, gains don't offset them, any player counts) —
 * `countPlayersWith(Each, lifeLostThisTurn() >= 4) >= 1`, an intervening "if" (Y'shtola's shape).
 */
val KnightOfTheEbonLegion = card("Knight of the Ebon Legion") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Knight"
    power = 1
    toughness = 2
    oracleText = "{2}{B}: This creature gets +3/+3 and gains deathtouch until end of turn.\n" +
        "At the beginning of your end step, if a player lost 4 or more life this turn, put a +1/+1 " +
        "counter on this creature. (Damage causes loss of life.)"

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.ModifyStats(3, 3, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.Self)
        description = "This creature gets +3/+3 and gains deathtouch until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.countPlayersWith(
                scope = Player.Each,
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.lifeLostThisTurn(),
                    ComparisonOperator.GTE,
                    4,
                ),
            ),
            ComparisonOperator.GTE,
            1,
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "At the beginning of your end step, if a player lost 4 or more life this turn, " +
            "put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Alex Konstad"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29425426-7bf2-4872-aa35-c12c22801edd.jpg?1783932992"
        ruling("2019-07-12", "Knight of the Ebon Legion looks at your entire turn to determine whether a player lost 4 or more life. It doesn't matter how many players lost 4 or more life, only whether anyone did. It also doesn't matter whether Knight of the Ebon Legion was on the battlefield when they lost life.")
        ruling("2019-07-12", "It doesn't matter whether a player has 4 or less life than they started the turn with. If a player lost 4 life but also gained 6 life, that player still lost 4 life.")
        ruling("2019-07-12", "A player doesn't need to have lost 4 life all at once. If a player loses 2 life twice during a turn, that player lost 4 life during that turn, and Knight of the Ebon Legion receives a +1/+1 counter.")
        ruling("2019-07-12", "A player loses life if they pay life.")
    }
}

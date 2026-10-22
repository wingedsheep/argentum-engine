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
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Duelcraft Trainer (Innistrad: Midnight Hunt #16)
 * {3}{W}
 * Creature — Human Soldier
 * 3/3
 * First strike
 * Coven — At the beginning of combat on your turn, if you control three or more creatures with
 * different powers, target creature you control gains double strike until end of turn.
 *
 * Coven is an intervening-if (checked when the trigger fires and again on resolution): three or more
 * creatures you control with pairwise-different powers, counted as distinct power values. The
 * target is chosen as the trigger goes on the stack.
 */
val DuelcraftTrainer = card("Duelcraft Trainer") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "First strike\n" +
        "Coven — At the beginning of combat on your turn, if you control three or more creatures " +
        "with different powers, target creature you control gains double strike until end of turn."
    power = 3
    toughness = 3

    keywords(Keyword.FIRST_STRIKE)

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
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "John Stanko"
        flavorText = "\"It's all in the follow-through.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b75855c-88dc-4491-8045-080debfeb7b5.jpg?1783925660"
        ruling("2021-09-24", "Duelcraft Trainer's triggered ability checks to see if you have three or more creatures with different powers both when it triggers and as it tries to resolve. If you don't have three creatures with different powers as it tries to resolve, it will be removed from the stack.")
    }
}

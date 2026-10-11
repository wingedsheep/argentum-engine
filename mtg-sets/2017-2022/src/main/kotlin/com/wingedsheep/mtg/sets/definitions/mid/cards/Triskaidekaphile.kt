package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Triskaidekaphile
 * {1}{U}
 * Creature — Human Wizard
 * 1/3
 *
 * You have no maximum hand size.
 * At the beginning of your upkeep, if you have exactly thirteen cards in your hand, you win the game.
 * {3}{U}: Draw a card.
 */
val Triskaidekaphile = card("Triskaidekaphile") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "You have no maximum hand size.\n" +
        "At the beginning of your upkeep, if you have exactly thirteen cards in your hand, you win the game.\n" +
        "{3}{U}: Draw a card."

    staticAbility {
        ability = NoMaximumHandSize
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.cardsInYourHand(),
            ComparisonOperator.EQ,
            13
        )
        effect = Effects.WinGame(
            target = EffectTarget.Controller,
            message = "Triskaidekaphile: exactly thirteen cards in hand."
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U}")
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Slawomir Maniak"
        flavorText = "She's always willing to lend a hand. It just won't be her own."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6750e203-1215-4203-b5b8-3f1b18940839.jpg?1783925628"

        ruling("2021-09-24", "Triskaidekaphile's triggered ability will trigger only if you have exactly thirteen cards in your hand as your upkeep starts. If you have fewer cards in your hand, you won't be able to draw cards during your upkeep in time to cause the ability to trigger.")
    }
}

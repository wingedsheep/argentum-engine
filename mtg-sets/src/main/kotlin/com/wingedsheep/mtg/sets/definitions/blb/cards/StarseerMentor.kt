package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Costs

/**
 * Starseer Mentor
 * {3}{W}{B}
 * Creature — Bat Warlock
 * 3/5
 *
 * Flying, vigilance
 * At the beginning of your end step, if you gained or lost life this turn,
 * target opponent loses 3 life unless they sacrifice a nonland permanent of
 * their choice or discard a card.
 */
val StarseerMentor = card("Starseer Mentor") {
    manaCost = "{3}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Bat Warlock"
    power = 3
    toughness = 5
    oracleText = "Flying, vigilance\nAt the beginning of your end step, if you gained or lost life this turn, target opponent loses 3 life unless they sacrifice a nonland permanent of their choice or discard a card."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouGainedOrLostLifeThisTurn
        val opponent = target("opponent", Targets.Opponent)
        effect = PayOrSufferEffect(
            cost = Costs.pay.Choice(
                listOf(
                    Costs.pay.Sacrifice(filter = GameObjectFilter.Nonland),
                    Costs.pay.Discard()
                )
            ),
            suffer = LoseLifeEffect(
                amount = DynamicAmount.Fixed(3),
                target = opponent
            ),
            player = opponent
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Taras Susak"
        flavorText = "\"Each star is given a name befitting its grandeur. This one is called Aurax, the Night-Eater. That one is called Blinky.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b2f6dc5-9fe8-49c1-b24c-1d99ce1da619.jpg?1721427196"

        ruling("2024-07-26", "Starseer Mentor's ability cares whether you gained or lost life this turn, not how your life total changed.")
        ruling("2024-07-26", "While resolving Starseer Mentor's last ability, the target opponent chooses a card to be discarded without revealing it, chooses a nonland permanent to be sacrificed, or chooses to do neither. Then that player discards that card, sacrifices that permanent, or loses 3 life.")
    }
}

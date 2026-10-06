package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tivash, Gloom Summoner — Commander 2021 #45
 * {4}{B} · Legendary Creature — Human Warlock · 4/4
 *
 * Lifelink
 * At the beginning of your end step, if you gained life this turn, you may pay X life, where X is
 * the amount of life you gained this turn. If you do, create an X/X black Demon creature token
 * with flying.
 *
 * The end-step trigger is an intervening "if" (checked as the end step begins and again on
 * resolution). X is the total life gained this turn (not net of life lost, per the ruling), read on
 * resolution for both the payment and the token's size — you can't pay less for a smaller Demon.
 */
val TivashGloomSummoner = card("Tivash, Gloom Summoner") {
    manaCost = "{4}{B}"
    typeLine = "Legendary Creature — Human Warlock"
    power = 4
    toughness = 4
    oracleText = "Lifelink\n" +
        "At the beginning of your end step, if you gained life this turn, you may pay X life, where " +
        "X is the amount of life you gained this turn. If you do, create an X/X black Demon creature " +
        "token with flying."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouGainedLifeThisTurn
        val lifeGained = DynamicAmounts.lifeGainedThisTurn()
        effect = Effects.MayPay(
            cost = Effects.PayDynamicLife(lifeGained),
            then = Effects.CreateDynamicToken(
                dynamicPower = lifeGained,
                dynamicToughness = lifeGained,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Demon"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/c/6/c6df5992-9c1c-407d-9602-a6c659342a15.jpg?1783927202",
            ),
        )
        description = "At the beginning of your end step, if you gained life this turn, you may pay X " +
            "life, where X is the amount of life you gained this turn. If you do, create an X/X black " +
            "Demon creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3b2a152-7bb9-495d-ac84-516097754137.jpg?1783927596"
        ruling("2021-04-16", "Tivash's ability counts the total amount of life gained without considering any life you lost during that turn. For example, if you lost 3 life and gained 4 life earlier in the turn, you may pay 4 life to create a 4/4 Demon.")
        ruling("2021-04-16", "You can't pay less life than the amount of life you gained to create a smaller but less harmful Demon.")
        ruling("2021-04-16", "You need to gain life before the end step begins for the last ability to trigger.")
    }
}

package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * The Gaffer
 * {2}{W}
 * Legendary Creature — Halfling Peasant
 * 2/3
 * At the beginning of each end step, if you gained 3 or more life this turn, draw a card.
 *
 * Every player's end step, gated by the intervening-if
 * [Conditions.YouGainedLifeThisTurnAtLeast] — the turn's total life gained, regardless of
 * life lost or whether The Gaffer was on the battlefield at the time.
 */
val TheGaffer = card("The Gaffer") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Halfling Peasant"
    power = 2
    toughness = 3
    oracleText = "At the beginning of each end step, if you gained 3 or more life this turn, draw a card."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.YouGainedLifeThisTurnAtLeast(3)
        effect = Effects.DrawCards(1)
        description = "At the beginning of each end step, if you gained 3 or more life this turn, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Tomas Duchek"
        flavorText = "On the growing of vegetables, especially potatoes, Master Hamfast Gamgee was recognized as the leading authority by all in the neighborhood."
        imageUri = "https://cards.scryfall.io/normal/front/8/6/863ed552-dc6d-4910-b188-3cefc3ebf0ed.jpg?1786423840"
        ruling("2023-06-16", "You draw just one card, no matter how much life you've gained past 3 life.")
        ruling("2023-06-16", "The Gaffer's ability looks at how much life you've gained in the turn, even if it wasn't on the battlefield when you gained life. It doesn't care if you also lost life, even if you lost more life than you gained.")
        ruling("2023-06-16", "If you haven't gained 3 or more life by the time an end step begins, The Gaffer's ability won't trigger at all.")
    }
}

package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

/**
 * Markov Purifier
 * {1}{W}{B}
 * Creature — Vampire Cleric
 * 2/3
 *
 * Lifelink
 * At the beginning of your end step, if you gained life this turn, you may pay {2}. If you do,
 * draw a card.
 *
 * The end-step trigger uses an intervening-if ([Conditions.YouGainedLifeThisTurn]); on resolution
 * it offers a flat "you may pay {2}. If you do, draw a card" gate ([MayPayManaEffect]).
 */
val MarkovPurifier = card("Markov Purifier") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Vampire Cleric"
    power = 2
    toughness = 3
    oracleText = "Lifelink\n" +
        "At the beginning of your end step, if you gained life this turn, you may pay {2}. If you " +
        "do, draw a card."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouGainedLifeThisTurn
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{2}"),
            effect = Effects.DrawCards(1)
        )
        description = "At the beginning of your end step, if you gained life this turn, you may " +
            "pay {2}. If you do, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "312"
        artist = "Samuel Araya"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/533673f3-5e18-4630-bd71-001774d698a8.jpg?1782702974"
    }
}

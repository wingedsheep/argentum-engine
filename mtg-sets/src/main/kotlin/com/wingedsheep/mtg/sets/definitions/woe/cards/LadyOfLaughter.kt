package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lady of Laughter
 * {3}{W}{W}
 * Creature — Faerie Noble
 * 4/5
 *
 * Flying
 * Celebration — At the beginning of your end step, if two or more nonland permanents entered the
 * battlefield under your control this turn, draw a card.
 *
 * The triggered half of the Celebration ability word (CR 207.2c — italic flavor, no rules meaning),
 * the same intervening-'if' shape as [PestsOfHonor] one step later in the turn: [Conditions.Celebration]
 * is checked both when the end step begins and again as the ability resolves (CR 603.4). Per the WOE
 * rulings it is a pure past-events check — the two permanents needn't still be on the battlefield or
 * still be yours, and a third adds nothing.
 */
val LadyOfLaughter = card("Lady of Laughter") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Faerie Noble"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "Celebration — At the beginning of your end step, if two or more nonland permanents " +
        "entered the battlefield under your control this turn, draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.Celebration
        effect = Effects.DrawCards(1)
        description = "At the beginning of your end step, if two or more nonland permanents " +
            "entered the battlefield under your control this turn, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "309"
        artist = "Kai Carpenter"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c26714f9-d42f-4bac-b185-8943d1621444.jpg?1783915041"

        ruling(
            "2023-09-01",
            "Celebration abilities only care if two or more nonland permanents entered the " +
                "battlefield under your control in a turn. They won't get more powerful if more " +
                "than two permanents entered the battlefield under your control in a turn."
        )
        ruling(
            "2023-09-01",
            "The permanents that entered the battlefield don't need to remain on the battlefield or " +
                "under your control. Celebration abilities are checking for past events, not the " +
                "current game state."
        )
    }
}

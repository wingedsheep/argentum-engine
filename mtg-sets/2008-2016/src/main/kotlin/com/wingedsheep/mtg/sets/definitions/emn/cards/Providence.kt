package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.revealFromOpeningHand
import com.wingedsheep.sdk.model.Rarity

/**
 * Providence — Eldritch Moon #37
 * {5}{W}{W} · Sorcery
 *
 * The opening-hand reveal (CR 103.6b) follows Sphinx of Foresight / Devourer of Destiny, but
 * without `fireOnPlayer`: "the first upkeep" is the game's first upkeep, whoever's turn it is, so
 * the delayed trigger fires at the next upkeep step after it is created — turn 1's. Step-based
 * one-shots are consumed when they fire, and two revealed copies just set the life total to 26
 * twice (2016-07-13 ruling).
 */
val Providence = card("Providence") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "You may reveal this card from your opening hand. If you do, at the beginning of the " +
        "first upkeep, your life total becomes 26.\n" +
        "Your life total becomes 26."

    revealFromOpeningHand(
        Effects.CreateDelayedTrigger(
            step = Step.UPKEEP,
            effect = Effects.SetLifeTotal(26)
        )
    )

    spell {
        effect = Effects.SetLifeTotal(26)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Zack Stella"
        flavorText = "There is power in prayer, regardless of who is listening."
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e5edd8d-8e10-4414-a326-95a672dfcff7.jpg?1783937512"
        ruling(
            "2021-03-19",
            "A player's \"opening hand\" is the hand of cards the player has after all players have taken " +
                "mulligans. If players have any cards in hand that allow actions to be taken with them from a " +
                "player's opening hand, the starting player takes all such actions first in any order, followed " +
                "by each other player in turn order. Then the first turn begins."
        )
        ruling(
            "2016-07-13",
            "Revealing multiples of Providence from your opening hand won't cause your life total to become " +
                "32 or any number other than 26."
        )
        ruling(
            "2016-07-13",
            "For a player's life total to become 26, the player gains or loses the appropriate amount of life. " +
                "For example, if your life total is 4 when Providence resolves, it will cause you to gain 22 life; " +
                "alternatively, if your life total is 40 when it resolves, it will cause you to lose 14 life. Other " +
                "cards that interact with life gain or life loss will interact with this effect accordingly."
        )
    }
}

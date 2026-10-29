package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawStepFirstCard
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceDrawWith
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Teferi's Ageless Insight
 * {2}{U}{U}
 * Legendary Enchantment
 * If you would draw a card except the first one you draw in each of your draw steps, draw two
 * cards instead.
 *
 * Alhammarret's Archive's draw half: `ReplaceDrawWith(draw 2)` on your per-card draws, with
 * `DrawStepFirstCard.EXCLUDED` exempting exactly the first card you draw in each of your draw steps.
 */
val TeferisAgelessInsight = card("Teferi's Ageless Insight") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Enchantment"
    oracleText = "If you would draw a card except the first one you draw in each of your draw steps, " +
        "draw two cards instead."

    replacementEffect(
        ReplaceDrawWith(
            replacementEffect = Effects.DrawCards(2),
            appliesTo = EventPattern.DrawEvent(
                player = Player.You,
                firstInDrawStep = DrawStepFirstCard.EXCLUDED,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "76"
        artist = "Kieran Yanner"
        flavorText = "\"Fine, you know more than me. But do you have to be such a show-off about it?\"\n" +
            "—Chandra Nalaar, to Teferi"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c38d1722-96b0-4756-9da9-fe18b1c80649.jpg?1783930719"
        ruling(
            "2020-06-23",
            "If a spell or ability causes you to put a card into your hand without specifically using " +
                "the word \"draw,\" it's not a card drawn."
        )
        ruling(
            "2020-06-23",
            "If two or more replacement effects would apply to a card-drawing event, the player drawing " +
                "the card chooses the order in which to apply them."
        )
        ruling(
            "2020-06-23",
            "Because Teferi's Ageless Insight is legendary, it's unlikely that one player will control " +
                "two. However, if that happens, each card that player would draw after the first will " +
                "result in four cards being drawn. If they control three, they draw eight cards, and so on."
        )
    }
}

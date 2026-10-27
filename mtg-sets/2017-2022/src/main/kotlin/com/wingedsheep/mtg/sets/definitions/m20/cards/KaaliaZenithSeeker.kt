package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Kaalia, Zenith Seeker
 * {R}{W}{B}
 * Legendary Creature — Human Cleric
 * 3/3
 * Flying, vigilance
 * When Kaalia enters, look at the top six cards of your library. You may reveal an Angel card, a
 * Demon card, and/or a Dragon card from among them and put them into your hand. Put the rest on
 * the bottom of your library in a random order.
 *
 * Three sequential up-to-one picks ([GiftOfTheGargantuan]'s shape, one per type), each choosing
 * from what the previous picks left. That is exactly the ruling: a card with more than one of the
 * types counts as whichever one it was picked for, and can't fill two slots.
 */
val KaaliaZenithSeeker = card("Kaalia, Zenith Seeker") {
    manaCost = "{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Legendary Creature — Human Cleric"
    oracleText = "Flying, vigilance\n" +
        "When Kaalia enters, look at the top six cards of your library. You may reveal an Angel card, a " +
        "Demon card, and/or a Dragon card from among them and put them into your hand. Put the rest on the " +
        "bottom of your library in a random order."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(6))
            val (angel, afterAngel) = chooseUpToSplit(
                1, from = looked,
                filter = GameObjectFilter.Any.withSubtype("Angel"),
                prompt = "You may reveal an Angel card to put into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave",
                showAllCards = true,
            )
            val (demon, afterDemon) = chooseUpToSplit(
                1, from = afterAngel,
                filter = GameObjectFilter.Any.withSubtype("Demon"),
                prompt = "You may reveal a Demon card to put into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave",
                showAllCards = true,
            )
            val (dragon, rest) = chooseUpToSplit(
                1, from = afterDemon,
                filter = GameObjectFilter.Any.withSubtype("Dragon"),
                prompt = "You may reveal a Dragon card to put into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true,
            )
            toHand(angel, revealed = true)
            toHand(demon, revealed = true)
            toHand(dragon, revealed = true)
            toLibraryBottom(rest, order = CardOrder.Random)
        }
        description = "When Kaalia enters, look at the top six cards of your library. You may reveal an " +
            "Angel card, a Demon card, and/or a Dragon card from among them and put them into your hand. " +
            "Put the rest on the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "210"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f400655-f495-4b21-ab9e-57fe4d845d45.jpg?1783932951"

        ruling(
            "2019-07-12",
            "You can put up to three cards into your hand this way: one Angel card, one Demon card, and/or " +
                "one Dragon card. If a card has more than one of these types, you choose which type it counts as."
        )
    }
}

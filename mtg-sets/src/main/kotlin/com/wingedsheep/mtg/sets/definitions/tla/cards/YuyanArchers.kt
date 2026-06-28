package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Yuyan Archers
 * {1}{R}
 * Creature — Human Archer
 * 3/1
 * Reach
 * When this creature enters, you may discard a card. If you do, draw a card.
 */
val YuyanArchers = card("Yuyan Archers") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Archer"
    power = 3
    toughness = 1
    oracleText = "Reach\nWhen this creature enters, you may discard a card. If you do, draw a card."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(effect = IfYouDoEffect(action = Patterns.Hand.discardCards(1), ifYouDo = DrawCardsEffect(1)))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Domco."
        flavorText = "\"Their precision is legendary. The Yuyan can pin a fly to a tree from a hundred yards away without killing it.\"\n—Admiral Zhao"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99244462-a996-4a5b-91fb-947045647d6d.jpg?1764121103"
    }
}

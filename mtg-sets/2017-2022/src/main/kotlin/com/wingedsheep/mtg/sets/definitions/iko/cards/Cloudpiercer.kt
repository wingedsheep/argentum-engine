package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Cloudpiercer
 * {4}{R}
 * Creature — Dinosaur
 * 5/4
 * Mutate {3}{R}
 * Reach
 * Whenever this creature mutates, you may discard a card. If you do, draw a card.
 */
val Cloudpiercer = card("Cloudpiercer") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur"
    power = 5
    toughness = 4
    oracleText = "Mutate {3}{R} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Reach\n" +
        "Whenever this creature mutates, you may discard a card. If you do, draw a card."

    keywordAbility(KeywordAbility.mutate("{3}{R}"))
    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Effects.May(
            Effects.IfYouDo(Patterns.Hand.discardCards(1), Effects.DrawCards(1))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3ec5f005-d8fb-48b8-8ac5-74445cc83273.jpg?1783931052"
    }
}

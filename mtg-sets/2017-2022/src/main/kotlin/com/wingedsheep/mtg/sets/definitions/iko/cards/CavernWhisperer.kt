package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Cavern Whisperer
 * {4}{B}
 * Creature — Nightmare
 * 4/4
 * Mutate {3}{B}
 * Menace
 * Whenever this creature mutates, each opponent discards a card.
 */
val CavernWhisperer = card("Cavern Whisperer") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Nightmare"
    power = 4
    toughness = 4
    oracleText = "Mutate {3}{B} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever this creature mutates, each opponent discards a card."

    keywordAbility(KeywordAbility.mutate("{3}{B}"))
    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Patterns.Hand.eachOpponentDiscards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64ae7651-4a5c-41f3-b528-41b9b0f2e5d8.jpg?1783931065"
    }
}

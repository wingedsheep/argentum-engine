package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Dreamtail Heron
 * {4}{U}
 * Creature — Elemental Bird
 * 3/4
 * Mutate {3}{U}
 * Flying
 * Whenever this creature mutates, draw a card.
 */
val DreamtailHeron = card("Dreamtail Heron") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental Bird"
    power = 3
    toughness = 4
    oracleText = "Mutate {3}{U} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Flying\n" +
        "Whenever this creature mutates, draw a card."

    keywordAbility(KeywordAbility.mutate("{3}{U}"))
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca777d40-736a-4cd6-85c4-5afd52f2fadd.jpg?1783931077"
    }
}

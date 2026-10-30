package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Majestic Auricorn
 * {4}{W}
 * Creature — Unicorn
 * 4/4
 * Mutate {3}{W}
 * Vigilance
 * Whenever this creature mutates, you gain 4 life.
 */
val MajesticAuricorn = card("Majestic Auricorn") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Unicorn"
    power = 4
    toughness = 4
    oracleText = "Mutate {3}{W} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Vigilance\n" +
        "Whenever this creature mutates, you gain 4 life."

    keywordAbility(KeywordAbility.mutate("{3}{W}"))
    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Effects.GainLife(4)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Jonathan Kuo"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8df320c9-449a-457e-8688-f611aceb3352.jpg?1783931085"
    }
}

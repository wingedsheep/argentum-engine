package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.increment
import com.wingedsheep.sdk.model.Rarity

/**
 * Hungry Graffalon — Secrets of Strixhaven #151
 * {3}{G} · Creature — Giraffe · 3/4
 *
 * Reach
 * Increment (Whenever you cast a spell, if the amount of mana you spent is greater than this
 * creature's power or toughness, put a +1/+1 counter on this creature.)
 *
 * Increment (Secrets of Strixhaven): the `increment()` DSL adds the PREPARED-sibling INCREMENT
 * keyword plus the composed "whenever you cast a spell" intervening-if trigger that grows the
 * creature when the spell's mana spent exceeds `min(power, toughness)` (here, 3 — so a 4+ mana
 * spell adds the first counter).
 */
val HungryGraffalon = card("Hungry Graffalon") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giraffe"
    power = 3
    toughness = 4
    oracleText = "Reach\nIncrement (Whenever you cast a spell, if the amount of mana you spent " +
        "is greater than this creature's power or toughness, put a +1/+1 counter on this creature.)"

    keywords(Keyword.REACH)
    increment()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Raph Lomotan"
        flavorText = "The nature of the Paradox Gardens is such that the beasts that live there " +
            "are always exactly as tall as they need to be to get a snack."
        imageUri = "https://cards.scryfall.io/normal/front/0/3/030b1272-5990-4bc9-8fc1-82cc05602060.jpg?1775938030"
    }
}

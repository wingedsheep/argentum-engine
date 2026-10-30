package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Cubwarden
 * {3}{W}
 * Creature — Cat
 * 3/5
 * Mutate {2}{W}{W}
 * Lifelink
 * Whenever this creature mutates, create two 1/1 white Cat creature tokens with lifelink.
 */
val Cubwarden = card("Cubwarden") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    power = 3
    toughness = 5
    oracleText = "Mutate {2}{W}{W} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Lifelink\n" +
        "Whenever this creature mutates, create two 1/1 white Cat creature tokens with lifelink."

    keywordAbility(KeywordAbility.mutate("{2}{W}{W}"))
    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Cat"),
            keywords = setOf(Keyword.LIFELINK),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/d/7/d754e09c-efc0-4536-9f2a-6bd7e2f860ab.jpg?1783930949",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc322744-5d8f-448d-b868-381bb86b68f9.jpg?1783931091"
    }
}

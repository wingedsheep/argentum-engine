package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Strife Scholar // Awaken the Ages — Secrets of Strixhaven #131
 * {2}{R} · Creature — Orc Sorcerer · 3/2
 *
 * Ward—Pay 2 life.
 * This creature enters prepared. (While it's prepared, you may cast a copy of its spell.
 * Doing so unprepares it.)
 * //
 * Awaken the Ages — {5}{R}, Sorcery: Create two 2/2 red and white Spirit creature tokens.
 *
 * Prepare (Secrets of Strixhaven): the creature enters with the PREPARED keyword. Becoming
 * prepared creates a copy of its prepare spell ("Awaken the Ages") in exile that its controller
 * may cast for {5}{R}; casting that copy unprepares the creature. Modeled via
 * [com.wingedsheep.sdk.model.CardLayout.PREPARE] + the `prepare(name) { }` DSL.
 */
val StrifeScholar = card("Strife Scholar") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Sorcerer"
    power = 3
    toughness = 2
    oracleText = "Ward—Pay 2 life.\n" +
        "This creature enters prepared. (While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    keywords(Keyword.PREPARED)
    keywordAbility(KeywordAbility.wardLife(2))

    // Awaken the Ages — the prepare spell. Create two 2/2 red and white Spirit creature tokens.
    prepare("Awaken the Ages") {
        manaCost = "{5}{R}"
        typeLine = "Sorcery"
        oracleText = "Create two 2/2 red and white Spirit creature tokens."
        spell {
            effect = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.RED, Color.WHITE),
                creatureTypes = setOf("Spirit"),
                count = 2,
                imageUri = "https://cards.scryfall.io/normal/front/8/7/877f7ddb-ed70-41a0-b845-d9bf8ac65f9b.jpg?1775828448"
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8de79312-2046-425e-9919-49afe19be81b.jpg?1775937883"
    }
}

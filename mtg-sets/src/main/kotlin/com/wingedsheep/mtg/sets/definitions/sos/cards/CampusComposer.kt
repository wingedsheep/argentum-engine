package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost

/**
 * Campus Composer // Aqueous Aria — Secrets of Strixhaven #40
 * {3}{U} · Creature — Merfolk Bard · 3/4
 *
 * Ward {2}
 * This creature enters prepared. (While it's prepared, you may cast a copy of its spell.
 * Doing so unprepares it.)
 * //
 * Aqueous Aria — {4}{U}, Sorcery: Create a 3/3 blue and red Elemental creature token with flying.
 *
 * Prepare (Secrets of Strixhaven): the creature enters with the PREPARED keyword. Becoming
 * prepared creates a copy of its prepare spell ("Aqueous Aria") in exile that its controller
 * may cast for {4}{U}; casting that copy unprepares the creature. Modeled via
 * [com.wingedsheep.sdk.model.CardLayout.PREPARE] + the `prepare(name) { }` DSL.
 */
val CampusComposer = card("Campus Composer") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Bard"
    power = 3
    toughness = 4
    oracleText = "Ward {2}\nThis creature enters prepared. (While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    keywords(Keyword.PREPARED)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    // Aqueous Aria — the prepare spell. Create a 3/3 blue and red Elemental with flying.
    prepare("Aqueous Aria") {
        manaCost = "{4}{U}"
        typeLine = "Sorcery"
        oracleText = "Create a 3/3 blue and red Elemental creature token with flying."
        spell {
            effect = Effects.CreateToken(
                power = 3,
                toughness = 3,
                colors = setOf(Color.BLUE, Color.RED),
                creatureTypes = setOf("Elemental"),
                keywords = setOf(Keyword.FLYING),
                imageUri = "https://cards.scryfall.io/normal/front/b/5/b5b2df9c-228f-4441-a962-46b335bb356e.jpg?1782723481"
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "40"
        artist = "Madeline Boni"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fac8ac39-ecb4-4142-bf37-131c65660a9b.jpg?1778164968"
    }
}

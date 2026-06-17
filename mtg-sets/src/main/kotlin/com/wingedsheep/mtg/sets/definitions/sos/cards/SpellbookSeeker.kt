package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spellbook Seeker // Careful Study — Secrets of Strixhaven #68
 * {3}{U} · Creature — Bird Wizard · 3/3
 *
 * Flying
 * This creature enters prepared. (While it's prepared, you may cast a copy of its spell.
 * Doing so unprepares it.)
 * //
 * Careful Study — {U}, Sorcery: Draw two cards, then discard two cards.
 *
 * Prepare (Secrets of Strixhaven): the creature enters with the PREPARED keyword. Becoming
 * prepared creates a copy of its prepare spell ("Careful Study") in exile that its controller
 * may cast for {U}; casting that copy unprepares the creature. Modeled via
 * [com.wingedsheep.sdk.model.CardLayout.PREPARE] + the `prepare(name) { }` DSL.
 */
val SpellbookSeeker = card("Spellbook Seeker") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Wizard"
    power = 3
    toughness = 3
    oracleText = "Flying\nThis creature enters prepared. (While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    keywords(Keyword.FLYING, Keyword.PREPARED)

    // Careful Study — the prepare spell. Draw two cards, then discard two cards.
    prepare("Careful Study") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        oracleText = "Draw two cards, then discard two cards."
        spell {
            effect = Effects.Composite(
                Effects.DrawCards(2),
                Patterns.Hand.discardCards(2)
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc44eaa4-59a4-419e-b1d1-d92f354ff588.jpg?1775937383"
    }
}

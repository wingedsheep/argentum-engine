package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Orzhov Enforcer — Ravnica Allegiance #79
 * {1}{B} · Creature — Human Rogue · 1/2
 *
 * Deathtouch
 * Afterlife 1 (When this creature dies, create a 1/1 white and black Spirit creature token with flying.)
 *
 * Afterlife has no SDK keyword, so it is composed as its reminder text — a self-dies trigger that
 * creates the Spirit token — the same shape as Indebted Spirit.
 */
val OrzhovEnforcer = card("Orzhov Enforcer") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 2
    oracleText = "Deathtouch\n" +
        "Afterlife 1 (When this creature dies, create a 1/1 white and black Spirit creature token with flying.)"

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/4/5/45b3bdd7-b093-4bfd-8a9a-965e72bfefb3.jpg?1783933602",
        )
        description = "Afterlife 1 (When this creature dies, create a 1/1 white and black Spirit creature token with flying.)"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Matt Stewart"
        flavorText = "\"You'll pay what you owe, with your money or your life.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a72b49e-be14-485b-a467-31cbac9aa1c5.jpg?1783933692"
        ruling(
            "2019-01-25",
            "Because blockers are chosen all at once, you can't block with a creature with afterlife, wait for it to die, then block with the resulting Spirit tokens."
        )
    }
}

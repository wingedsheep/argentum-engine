package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ministrant of Obligation — Ravnica Allegiance #16
 * {2}{W} · Creature — Human Cleric · 2/1
 * Afterlife 2 (When this creature dies, create two 1/1 white and black Spirit creature tokens with flying.)
 *
 * Afterlife has no SDK keyword, so it is composed as its reminder text — a self-dies trigger that
 * creates the Spirits (Indebted Spirit).
 */
val MinistrantOfObligation = card("Ministrant of Obligation") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 1
    oracleText = "Afterlife 2 (When this creature dies, create two 1/1 white and black Spirit creature " +
        "tokens with flying.)"

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLACK),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/4/5/45b3bdd7-b093-4bfd-8a9a-965e72bfefb3.jpg?1783933602",
        )
        description = "Afterlife 2 (When this creature dies, create two 1/1 white and black Spirit " +
            "creature tokens with flying.)"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Bastien L. Deharme"
        flavorText = "When the price of your sins comes due, the Church of Deals is there to collect."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf2a093a-25f0-4247-94dc-550865d86317.jpg?1783933721"
        ruling(
            "2019-01-25",
            "Because blockers are chosen all at once, you can't block with a creature with afterlife, wait for " +
                "it to die, then block with the resulting Spirit tokens."
        )
    }
}

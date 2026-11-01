package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Syndicate Messenger — Ravnica Allegiance #25
 * {3}{W} · Creature — Bird · 2/3
 * Flying
 * Afterlife 1 (When this creature dies, create a 1/1 white and black Spirit creature token with flying.)
 *
 * Afterlife is composed as its reminder text (Knight of Sorrows / Ministrant of Obligation).
 */
val SyndicateMessenger = card("Syndicate Messenger") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "Afterlife 1 (When this creature dies, create a 1/1 white and black Spirit creature token with flying.)"

    keywords(Keyword.FLYING)

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
        description = "Afterlife 1 (When this creature dies, create a 1/1 white and black Spirit " +
            "creature token with flying.)"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Viktor Titov"
        flavorText = "\"Many wings haunt the skies these days. Few of them bear good tidings.\"\n—Lavinia"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10273046-4c74-42d8-afaa-6cfbe0bd4e8f.jpg?1783933716"
    }
}

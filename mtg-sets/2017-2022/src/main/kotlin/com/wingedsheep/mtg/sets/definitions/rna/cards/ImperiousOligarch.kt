package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Imperious Oligarch — Ravnica Allegiance #184
 * {W}{B} · Creature — Human Cleric · 2/1
 *
 * Vigilance
 * Afterlife 1 (When this creature dies, create a 1/1 white and black Spirit creature token with flying.)
 *
 * Afterlife is composed as its reminder text — a self-dies trigger creating the Spirit token — the
 * same shape as Orzhov Enforcer.
 */
val ImperiousOligarch = card("Imperious Oligarch") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 1
    oracleText = "Vigilance\nAfterlife 1 (When this creature dies, create a 1/1 white and black Spirit " +
        "creature token with flying.)"

    keywords(Keyword.VIGILANCE)

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
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Johannes Voss"
        flavorText = "The rights of ghosts are strictly protected under Orzhov bylaws, and those who enforce " +
            "them can count on the ghosts' assistance."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd27ba9e-5b9c-468f-9a44-bf2e89138e72.jpg?1783933646"
    }
}

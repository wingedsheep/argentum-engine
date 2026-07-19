package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Thor Odinson
 * {3}{R}{W}
 * Legendary Creature — God Warrior Hero — Uncommon (MSH #234)
 * 4/4
 *
 * "Flying, vigilance, prowess, prowess"
 *
 * Implementation: prowess is a keyword ability backed by an intrinsic triggered ability, and this
 * card has **two** instances of it. Multiple instances of prowess are separate abilities that each
 * trigger on a noncreature spell (net +2/+2 per spell). The `prowess()` builder is
 * therefore called twice: the keyword itself is a set entry (displayed once), while the two
 * triggered abilities are both registered. Flying and vigilance are plain engine keywords.
 */
val ThorOdinson = card("Thor Odinson") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — God Warrior Hero"
    power = 4
    toughness = 4
    oracleText = "Flying, vigilance, prowess, prowess (Whenever you cast a noncreature spell, " +
        "this creature gets +1/+1 until end of turn twice.)"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)
    // Two separate instances of prowess — each triggers on its own.
    prowess()
    prowess()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Sean Vo"
        flavorText = "\"Now, my hammer shall speak for me . . . in a voice of thunder!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e06f730-ae30-4ef2-88b1-073e02afdb25.jpg?1783902895"
    }
}

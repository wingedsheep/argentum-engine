package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked

/**
 * Elusive Krasis — Gatecrash #160
 * {1}{G}{U} · Creature — Fish Mutant · 0/4
 *
 * This creature can't be blocked.
 * Evolve
 */
val ElusiveKrasis = card("Elusive Krasis") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Fish Mutant"
    power = 0
    toughness = 4
    oracleText = "This creature can't be blocked.\n" +
        "Evolve (Whenever a creature you control enters, if that creature has greater power or toughness " +
        "than this creature, put a +1/+1 counter on this creature.)"

    staticAbility {
        ability = CantBeBlocked()
    }

    evolve()

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd62e422-e5e2-4736-9ed3-d2dc693f6f8f.jpg"
    }
}

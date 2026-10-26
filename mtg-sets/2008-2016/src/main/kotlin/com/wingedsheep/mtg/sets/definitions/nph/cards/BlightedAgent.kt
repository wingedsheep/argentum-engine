package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Blighted Agent
 * {1}{U}
 * Creature — Phyrexian Human Rogue
 * 1/1
 *
 * Infect
 * This creature can't be blocked.
 */
val BlightedAgent = card("Blighted Agent") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Phyrexian Human Rogue"
    power = 1
    toughness = 1
    oracleText = "Infect (This creature deals damage to creatures in the form of -1/-1 counters and to players in the form of poison counters.)\n" +
        "This creature can't be blocked."

    keywords(Keyword.INFECT)
    flags(AbilityFlag.CANT_BE_BLOCKED)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Anthony Francisco"
        flavorText = "\"Urabrask may suspect our surveillance, but he cannot stop it.\"\n—Avaricta, Gitaxian sective"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cddaebde-a060-4510-8c97-68432d931987.jpg?1783941323"
    }
}

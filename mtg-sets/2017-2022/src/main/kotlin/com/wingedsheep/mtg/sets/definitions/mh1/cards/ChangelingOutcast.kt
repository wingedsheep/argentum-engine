package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CantBlock

/**
 * Changeling Outcast — Modern Horizons #82 (canonical printing)
 * {B} · Creature — Shapeshifter · 1/1
 *
 * Changeling (This card is every creature type.)
 * This creature can't block and can't be blocked.
 *
 * Tormented Soul with changeling: [Keyword.CHANGELING] plus the [CantBlock] / [CantBeBlocked] pair.
 */
val ChangelingOutcast = card("Changeling Outcast") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Shapeshifter"
    power = 1
    toughness = 1
    oracleText = "Changeling (This card is every creature type.)\n" +
        "This creature can't block and can't be blocked."

    keywords(Keyword.CHANGELING)

    staticAbility {
        ability = CantBlock()
    }

    staticAbility {
        ability = CantBeBlocked()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Micah Epstein"
        flavorText = "A mercurial face sows distrust. Distrust reaps a lonely life."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08151d00-513a-4957-aa16-01c0f0245112.jpg?1783933130"
    }
}

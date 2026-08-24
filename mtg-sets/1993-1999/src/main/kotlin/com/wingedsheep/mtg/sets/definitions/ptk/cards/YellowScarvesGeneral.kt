package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock

/**
 * Yellow Scarves General
 * {3}{R}
 * Creature — Human Soldier
 * 2/2
 * Horsemanship
 * This creature can't block.
 */
val YellowScarvesGeneral = card("Yellow Scarves General") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText =
        "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\n" +
        "This creature can't block."

    keywords(Keyword.HORSEMANSHIP)

    staticAbility {
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Chen Weidong"
        flavorText = "Zhang Jue, leader of the Yellow Scarves rebellion, was a Taoist master and tutored his soldiers in those arts."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06e98691-6227-41f2-a3f4-3131b07a3a6f.jpg"
    }
}

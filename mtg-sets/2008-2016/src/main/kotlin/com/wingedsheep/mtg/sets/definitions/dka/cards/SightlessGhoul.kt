package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock

/**
 * Sightless Ghoul
 * {3}{B}
 * Creature — Zombie Soldier
 * 2/2
 * This creature can't block.
 * Undying
 */
val SightlessGhoul = card("Sightless Ghoul") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Soldier"
    power = 2
    toughness = 2
    oracleText = "This creature can't block.\n" +
        "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it.)"

    keywords(Keyword.UNDYING)

    staticAbility {
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Svetlin Velinov"
        flavorText = "It blindly marches on Thraben's gates, guided by its master's direful will."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/018bd4ae-cdea-410d-9ce6-6a70f12de966.jpg"
    }
}

package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Phantom Warrior
 * {1}{U}{U}
 * Creature - Illusion Warrior
 * 2/2
 * Phantom Warrior can't be blocked.
 */
val PhantomWarrior = card("Phantom Warrior") {
    manaCost = "{1}{U}{U}"
    typeLine = "Creature — Illusion Warrior"
    power = 2
    toughness = 2

    keywords(Keyword.UNBLOCKABLE)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a375a88-1cbc-4a9b-b1d0-ef90f12c2a08.jpg"
    }
}

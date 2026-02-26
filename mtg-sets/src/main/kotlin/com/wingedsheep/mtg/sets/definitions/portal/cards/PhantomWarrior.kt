package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.AbilityFlag
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

    flags(AbilityFlag.CANT_BE_BLOCKED)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "65"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6dbcb0df-d1cc-4718-ba1e-b590852cce20.jpg"
    }
}

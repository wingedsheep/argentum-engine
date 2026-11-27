package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Hunted Ghoul
 * {B}
 * Creature — Zombie
 * 1/2
 * This creature can't block Humans.
 *
 * "Can't block Humans" is expressed as its exact complement, [CanOnlyBlockCreaturesWith] over
 * non-Human creatures (same shape as Burden of Proof's "can't block Detectives").
 */
val HuntedGhoul = card("Hunted Ghoul") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 2
    oracleText = "This creature can't block Humans."

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(
            blockerFilter = GameObjectFilter.Creature.notSubtype(Subtype.HUMAN)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Ryan Pancoast"
        flavorText = "Even after Hanweir was saved, Gisa's lost ghouls would shamble through, still " +
            "following the twisted orders of their imprisoned master."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb443cf7-70bd-4df7-b446-107e099c19e3.jpg?1783940695"
    }
}

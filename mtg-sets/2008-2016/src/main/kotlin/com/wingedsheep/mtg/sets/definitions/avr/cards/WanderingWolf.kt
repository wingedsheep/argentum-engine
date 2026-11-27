package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByCreaturesWithLessPower

/**
 * Wandering Wolf — Avacyn Restored #202.
 * {1}{G} Creature — Wolf 2/1
 *
 * "Creatures with power less than this creature's power can't block it." is the attacker-side static
 * [CantBeBlockedByCreaturesWithLessPower] (Howlgeist, Shrill Howler), compared on projected power when
 * blockers are declared.
 */
val WanderingWolf = card("Wandering Wolf") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf"
    oracleText = "Creatures with power less than this creature's power can't block it."
    power = 2
    toughness = 1

    staticAbility {
        ability = CantBeBlockedByCreaturesWithLessPower()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "202"
        artist = "Tomasz Jedruszek"
        flavorText = "Humans still feared wolves, though no connection between them and the curse of " +
            "lycanthropy had ever been proved."
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac606ad5-b8d0-4c93-a9de-5e41229a8229.jpg?1783940658"
        ruling(
            "2012-05-01",
            "The comparison of power is only done when blockers are declared. Increasing the " +
                "power of a blocking creature (or decreasing the power of Wandering Wolf) after this point " +
                "won’t cause any creature to stop blocking or become unblocked."
        )
    }
}

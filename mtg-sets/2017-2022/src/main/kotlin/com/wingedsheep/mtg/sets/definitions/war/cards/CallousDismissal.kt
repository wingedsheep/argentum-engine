package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Callous Dismissal — War of the Spark #44 (canonical printing)
 * {1}{U}
 * Sorcery
 * Return target nonland permanent to its owner's hand.
 * Amass Zombies 1.
 *
 * If the target is illegal on resolution the spell doesn't resolve and you don't amass (2023-06-16 ruling).
 */
val CallousDismissal = card("Callous Dismissal") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return target nonland permanent to its owner's hand.\n" +
        "Amass Zombies 1. (Put a +1/+1 counter on an Army you control. It's also a Zombie. " +
        "If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

    spell {
        val t = target(TargetFilter.NonlandPermanent)
        effect = Effects.ReturnToHand(t) then Effects.Amass(1, "Zombie")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Mathias Kollros"
        flavorText = "\"They make it clear we are nothing to them.\"\n—Lavinia"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66cef326-339c-49e5-9905-21a1603a46ae.jpg?1783933467"
        ruling(
            "2023-06-16",
            "Some spells and abilities that amass Zombies may require targets. If each target chosen is an illegal target as that spell or ability tries to resolve, it won't resolve. You won't amass Zombies."
        )
    }
}

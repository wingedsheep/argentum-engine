package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Revenge of the Drowned
 * {3}{U}
 * Instant
 * Target creature's owner puts it on their choice of the top or bottom of their library. You create
 * a 2/2 black Zombie creature token with decayed.
 *
 * The owner (not the caster) picks the library position; the token goes to the spell's controller.
 */
val RevengeOfTheDrowned = card("Revenge of the Drowned") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature's owner puts it on their choice of the top or bottom of their library. " +
        "You create a 2/2 black Zombie creature token with decayed. (It can't block. When it attacks, " +
        "sacrifice it at end of combat.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.PutOnTopOrBottomOfLibrary(creature) then Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            keywords = setOf(Keyword.DECAYED),
            imageUri = "https://cards.scryfall.io/normal/front/6/a/6adb8607-1066-451d-a719-74ad32358278.jpg?1783925226",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Chris Cold"
        flavorText = "A heavy net doesn't always mean a good catch."
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93be7d72-927a-40ce-8de4-04a3f21c134b.jpg?1783925632"
    }
}

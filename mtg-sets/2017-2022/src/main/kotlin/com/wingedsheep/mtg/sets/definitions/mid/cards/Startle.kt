package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Startle
 * {1}{U}
 * Instant
 * Target creature gets -2/-0 until end of turn. Create a 2/2 black Zombie creature token with
 * decayed.
 * Draw a card.
 */
val Startle = card("Startle") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature gets -2/-0 until end of turn. Create a 2/2 black Zombie creature token " +
        "with decayed. (It can't block. When it attacks, sacrifice it at end of combat.)\nDraw a card."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-2, 0, creature) then Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            keywords = setOf(Keyword.DECAYED),
            imageUri = "https://cards.scryfall.io/normal/front/6/a/6adb8607-1066-451d-a719-74ad32358278.jpg?1783925226",
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35f68eb3-9b40-4967-a52e-f95aec6e3fbf.jpg?1783925628"
    }
}

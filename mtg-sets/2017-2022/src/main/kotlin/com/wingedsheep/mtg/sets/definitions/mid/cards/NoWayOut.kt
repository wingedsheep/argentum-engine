package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * No Way Out
 * {2}{B}
 * Sorcery
 * Target opponent discards two cards. You create a 2/2 black Zombie creature token with decayed.
 */
val NoWayOut = card("No Way Out") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards two cards. You create a 2/2 black Zombie creature token with decayed. " +
        "(It can't block. When it attacks, sacrifice it at end of combat.)"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Discard(2, opponent) then Effects.CreateToken(
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
        collectorNumber = "116"
        artist = "Sebastian Giacobino"
        flavorText = "In the maze-like passages of the Erdwal, a wrong turn can be fatal."
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9ef6a68-60a4-4a20-8b57-8b7a7c5ed0c6.jpg?1783925611"
    }
}

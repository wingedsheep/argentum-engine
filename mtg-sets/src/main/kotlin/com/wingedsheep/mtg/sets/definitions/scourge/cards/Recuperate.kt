package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity


/**
 * Recuperate
 * {3}{W}
 * Instant
 * Target player gains 5 life.
 */
val Recuperate = card("Recuperate") {
    manaCost = "{3}{W}"
    typeLine = "Instant"
    oracleText = "Target player gains 5 life."

    spell {
        val t = target("target", Targets.Player)
        effect = Effects.GainLife(5, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Mark Romanoski"
        flavorText = "\"Pain is the body's way of telling you it's still alive.\"\n—Battlefield medic"
        imageUri = "https://cards.scryfall.io/large/front/a/5/a5945397-0906-48dd-80d1-c65bfa2b31a6.jpg?1562533088"
    }
}

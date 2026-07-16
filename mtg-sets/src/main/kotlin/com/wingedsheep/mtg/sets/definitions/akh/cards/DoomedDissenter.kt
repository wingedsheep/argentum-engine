package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Doomed Dissenter
 * {1}{B}
 * Creature — Human
 * 1/1
 *
 * When this creature dies, create a 2/2 black Zombie creature token.
 *
 * Canonical printing is Amonkhet (earliest real set); Innistrad: Crimson Vow gets a reprint row.
 */
val DoomedDissenter = card("Doomed Dissenter") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, create a 2/2 black Zombie creature token."

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            imageUri = "https://cards.scryfall.io/normal/front/b/5/b5bd6905-79be-4d2c-a343-f6e6a181b3e6.jpg?1783936411"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "87"
        artist = "Tony Foti"
        flavorText = "There is only one fate left to those banished from the God-Pharaoh's city."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1c0b645-de43-46d0-84c1-03f295def217.jpg?1783936508"
    }
}

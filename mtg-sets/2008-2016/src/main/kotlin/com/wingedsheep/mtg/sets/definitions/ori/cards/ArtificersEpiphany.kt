package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Artificer's Epiphany — Magic Origins #45 (canonical printing)
 * {2}{U} · Instant
 *
 * Draw two cards. If you control no artifacts, discard a card.
 *
 * The artifact check is made at resolution, after the draw (CR 608.2). Per the ruling, controlling
 * an artifact means you can't discard even if you want to — so the discard is gated, not optional.
 */
val ArtificersEpiphany = card("Artificer's Epiphany") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Draw two cards. If you control no artifacts, discard a card."

    spell {
        effect = Effects.DrawCards(2) then Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Artifact, negate = true),
            then = Effects.Discard(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Kieran Yanner"
        flavorText = "The artificers of Kaladesh strive ceaselessly for perfection, progress, and the ultimate expression of elegance."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c847774-c76d-44bd-acf6-bc1bc865ac77.jpg?1783938354"
        ruling(
            "2015-06-22",
            "If you control at least one artifact as Artificer's Epiphany resolves, you can't discard a card, even if you want to.",
        )
    }
}

package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * You Are Already Dead — Kamigawa: Neon Dynasty #129 (canonical printing)
 * {B} · Instant
 *
 * Destroy target creature that was dealt damage this turn.
 * Draw a card.
 */
val YouAreAlreadyDead = card("You Are Already Dead") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature that was dealt damage this turn.\nDraw a card."

    spell {
        val t = target(TargetFilter.Creature.wasDealtDamageThisTurn())
        effect = Effects.Destroy(t) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "129"
        artist = "Zoltan Boros"
        flavorText = "So swift and sharp was his blade that the ogre never even felt the cut."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/768727ce-4f84-4527-8d69-3c9b7877b748.jpg?1783923872"
    }
}

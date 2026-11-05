package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Time to Reflect — Amonkhet #33
 * {W} · Instant · Uncommon
 *
 * Exile target creature that blocked or was blocked by a Zombie this turn.
 *
 * The partner is read as it stood when the block was declared, so an embalmed Zombie token that
 * has since died still counts.
 */
val TimeToReflect = card("Time to Reflect") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature that blocked or was blocked by a Zombie this turn."

    spell {
        val t = target(
            TargetFilter(
                GameObjectFilter.Creature.blockedOrWasBlockedByThisTurn(GameObjectFilter.Creature.withSubtype("Zombie"))
            )
        )
        effect = Effects.Exile(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Daarken"
        flavorText = "\"Occasionally, there are those who refuse to appreciate all that the God-Pharaoh provides for us.\"\n—Temmet, vizier of Naktamun"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b60ec4f7-7cf1-4b97-906b-536ab89b12be.jpg?1783936533"
    }
}

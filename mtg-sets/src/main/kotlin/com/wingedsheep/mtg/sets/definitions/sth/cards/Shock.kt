package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Shock
 * {R}
 * Instant
 * Shock deals 2 damage to any target.
 *
 * Canonical printing: Stronghold (1998) is Shock's earliest real-expansion
 * printing, so the [com.wingedsheep.sdk.model.CardDefinition] lives here. Later
 * printings (e.g. Onslaught) contribute only a `Printing(...)` row.
 */
val Shock = card("Shock") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Shock deals 2 damage to any target."

    spell {
        val t = target("target", AnyTarget())
        effect = DealDamageEffect(2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Randy Gallegos"
        flavorText = "Lightning tethers souls to the world.\n—Kor saying"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9b2ff2a-6dfe-4635-8da2-22d525e82b94.jpg?1562597849"
    }
}

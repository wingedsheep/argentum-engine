package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Inordinate Rage — Zendikar Rising #144
 * {1}{R} · Instant
 *
 * Target creature gets +3/+2 until end of turn. Scry 1.
 */
val InordinateRage = card("Inordinate Rage") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+2 until end of turn. Scry 1."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 2, t) then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Tomasz Jedruszek"
        flavorText = "\"In my years wandering this land I've seen a veritable menagerie of fantastic creatures, " +
            "but one that has eluded me thus far is the fabled 'easygoing minotaur.'\"\n—Ko, expedition wizard"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0f62bdc-50a0-41a0-ac37-9b0eeb21b6aa.jpg"
    }
}

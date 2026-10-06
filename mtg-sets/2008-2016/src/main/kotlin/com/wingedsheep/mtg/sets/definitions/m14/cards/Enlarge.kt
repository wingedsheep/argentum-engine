package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Enlarge
 * {3}{G}{G}
 * Sorcery
 * Target creature gets +7/+7 and gains trample until end of turn. It must be blocked this turn
 * if able.
 *
 * "Must be blocked if able" needs only one blocker (see ruling), so the requirement is
 * [Effects.MustBeBlocked] with `allCreatures = false`, not the Lure-style every-able-blocker form.
 */
val Enlarge = card("Enlarge") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +7/+7 and gains trample until end of turn. It must be " +
        "blocked this turn if able. (A creature with trample can deal excess combat damage to " +
        "the player or planeswalker it's attacking.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(7, 7, t) then
            Effects.GrantKeyword(Keyword.TRAMPLE, t) then
            Effects.MustBeBlocked(t, allCreatures = false)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Michael Komarck"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b46cd181-59d8-4d4c-a8b6-e6b38704009c.jpg?1783939906"
        ruling(
            "2020-08-07",
            "Only one creature is required to block the affected creature. Other creatures may " +
                "also block it and are free to block other creatures or not block at all.",
        )
        ruling(
            "2020-08-07",
            "If each creature the defending player controls can't block for any reason (such as " +
                "being tapped), then the affected creature isn't blocked. If there's a cost " +
                "associated with blocking the affected creature, the defending player isn't " +
                "forced to pay that cost, so it doesn't have to be blocked in that case either.",
        )
    }
}

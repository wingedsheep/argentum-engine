package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ral's Dispersal — Guilds of Ravnica #266 (canonical printing; planeswalker deck)
 * {3}{U}{U} · Instant
 *
 * Return target creature to its owner's hand. You may search your library and/or graveyard for a
 * card named Ral, Caller of Storms, reveal it, and put it into your hand. If you search your library
 * this way, shuffle.
 *
 * The search half is the Oko's Hospitality planeswalker-deck tutor. With its only target illegal
 * the spell doesn't resolve, so there's no search either (ruling) — the engine's fizzle rule.
 */
val RalsDispersal = card("Ral's Dispersal") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target creature to its owner's hand. You may search your library and/or " +
        "graveyard for a card named Ral, Caller of Storms, reveal it, and put it into your hand. If you " +
        "search your library this way, shuffle."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ReturnToHand(creature) then Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Ral, Caller of Storms"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "266"
        artist = "Lake Hurwitz"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2660fae-6459-446d-85dc-3c9126c81cb3.jpg?1783934094"
        ruling("2018-10-05", "If the target creature becomes an illegal target for Ral’s Dispersal, the spell doesn’t resolve. You don’t search for Ral, Caller of Storms.")
    }
}

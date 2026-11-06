package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Liberating Combustion — Kaladesh #267 (Planeswalker Deck)
 * {4}{R} · Sorcery
 *
 * Liberating Combustion deals 6 damage to target creature. You may search your library and/or
 * graveyard for a card named Chandra, Pyrogenius, reveal it, and put it into your hand. If you
 * search your library this way, shuffle.
 *
 * The planeswalker-deck tutor shape (Sun-Blessed Mount, Angrath's Fury). If the creature becomes an
 * illegal target the spell doesn't resolve, so no search happens (ruling).
 */
val LiberatingCombustion = card("Liberating Combustion") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Liberating Combustion deals 6 damage to target creature. You may search your library " +
        "and/or graveyard for a card named Chandra, Pyrogenius, reveal it, and put it into your hand. " +
        "If you search your library this way, shuffle."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(6, creature) then
            Patterns.Library.searchLibraryAndOrGraveyard(
                filter = GameObjectFilter.Any.named("Chandra, Pyrogenius"),
                destination = SearchDestination.HAND,
                reveal = true,
                optional = true
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "267"
        inBooster = false
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9d2156e-2d54-440e-b0fd-7bef702afbfc.jpg?1783937138"

        ruling("2016-09-20", "If the target creature becomes an illegal target for Liberating Combustion, the spell won't resolve and none of its effects will happen. You won't search for Chandra, Pyrogenius.")
    }
}

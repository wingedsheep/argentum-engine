package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tezzeret's Betrayal — Aether Revolt #191 (Planeswalker Deck)
 * {3}{U}{B} · Sorcery
 *
 * Destroy target creature. You may search your library and/or graveyard for a card named
 * Tezzeret, Master of Metal, reveal it, and put it into your hand. If you search your library this
 * way, shuffle.
 *
 * The planeswalker-deck tutor shape (Liberating Combustion, Angrath's Fury). If the creature becomes
 * an illegal target the spell doesn't resolve, so no search happens (ruling).
 */
val TezzeretsBetrayal = card("Tezzeret's Betrayal") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. You may search your library and/or graveyard for a card " +
        "named Tezzeret, Master of Metal, reveal it, and put it into your hand. If you search your " +
        "library this way, shuffle."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then
            Patterns.Library.searchLibraryAndOrGraveyard(
                filter = GameObjectFilter.Any.named("Tezzeret, Master of Metal"),
                destination = SearchDestination.HAND,
                reveal = true,
                optional = true
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        inBooster = false
        artist = "Tyler Jacobson"
        flavorText = "\"You can't hope to comprehend the plans that are in motion.\"\n—Tezzeret"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d71efa6-5de8-476f-86ce-0790956e574f.jpg?1783936714"

        ruling("2017-02-09", "If the target creature becomes an illegal target for Tezzeret's Betrayal, the spell won't resolve and none of its effects will happen. You won't search for Tezzeret, Master of Metal.")
    }
}

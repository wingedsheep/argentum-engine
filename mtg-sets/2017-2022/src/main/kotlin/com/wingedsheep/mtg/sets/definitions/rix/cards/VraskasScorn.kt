package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Vraska's Scorn — Rivals of Ixalan #200 (planeswalker deck)
 * {2}{B}{B} · Sorcery
 *
 * Target opponent loses 4 life. You may search your library and/or graveyard for a card named
 * Vraska, Scheming Gorgon, reveal it, and put it into your hand. If you search your library this
 * way, shuffle.
 *
 * The search half is the planeswalker-deck tutor (Vraska's Stoneglare's optional shape); with its
 * only target illegal the spell doesn't resolve, so there's no search either.
 */
val VraskasScorn = card("Vraska's Scorn") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent loses 4 life. You may search your library and/or graveyard for a card " +
        "named Vraska, Scheming Gorgon, reveal it, and put it into your hand. If you search your library " +
        "this way, shuffle."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(4, opponent) then
            Patterns.Library.searchLibraryAndOrGraveyard(
                filter = GameObjectFilter.Any.named("Vraska, Scheming Gorgon"),
                destination = SearchDestination.HAND,
                reveal = true,
                optional = true
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "200"
        inBooster = false
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/569ff807-365d-4f25-9fad-86617ed1c8f2.jpg?1783935258"
    }
}

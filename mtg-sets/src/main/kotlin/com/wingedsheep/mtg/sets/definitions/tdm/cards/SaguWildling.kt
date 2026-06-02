package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Sagu Wildling // Roost Seek
 * {4}{G} // {G}
 * Creature — Dragon // Sorcery — Omen
 * 3/3
 *
 * Sagu Wildling:
 *   Flying
 *   When this creature enters, you gain 3 life.
 *
 * Roost Seek — {G}, Sorcery — Omen:
 *   Search your library for a basic land card, reveal it, put it into your hand, then shuffle.
 *   (Also shuffle this card.)
 *
 * Omen (CR 715, modeled on the Adventure layout): casting Roost Seek resolves its search, then
 * exiles the card and lets the caster cast Sagu Wildling later from exile. The "(Also shuffle
 * this card.)" reminder is handled by the Omen/Adventure exile-and-recast flow — the omen half
 * leaves the stack into exile rather than the graveyard.
 */
val SaguWildling = card("Sagu Wildling") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dragon"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, you gain 3 life."

    keywords(Keyword.FLYING)

    // When this creature enters, you gain 3 life.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(3)
        description = "you gain 3 life."
    }

    // Roost Seek — Omen. Search your library for a basic land card, reveal it, put it into your
    // hand, then shuffle.
    adventure("Roost Seek") {
        manaCost = "{G}"
        typeLine = "Sorcery — Omen"
        oracleText = "Search your library for a basic land card, reveal it, put it into your hand, " +
            "then shuffle. (Also shuffle this card.)"
        spell {
            effect = LibraryPatterns.searchLibrary(
                filter = Filters.BasicLand,
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = true,
                reveal = true
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "157"
        artist = "Gaboleps"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8b43b00-f4d1-436c-bf3f-6d414cd4ce38.jpg?1743204593"
    }
}

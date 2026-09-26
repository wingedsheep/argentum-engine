package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hinder — Champions of Kamigawa #65 (canonical printing)
 * {1}{U}{U} · Instant
 *
 * Counter target spell. If that spell is countered this way, put that card on your choice of the
 * top or bottom of its owner's library instead of into that player's graveyard.
 *
 * A real counter into the library (`CounterDestination.Library`), not a "put target spell on top
 * or bottom" move: an uncounterable spell is untouched and no choice is asked, and "whenever a
 * spell is countered" triggers see it. Hinder's controller chooses the end, not the spell's owner.
 */
val Hinder = card("Hinder") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. If that spell is countered this way, put that card on your " +
        "choice of the top or bottom of its owner's library instead of into that player's graveyard."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpellToLibrary(LibraryChoicePosition.Top, LibraryChoicePosition.Bottom)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Wayne Reynolds"
        flavorText = "\"Do not react to force in kind. Turn it aside. Direct it to where it can do no harm.\"\n" +
            "—Meloku the Clouded Mirror"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc7befed-805b-4a02-a87d-7df3a95db8a0.jpg?1783944326"
        ruling(
            "2020-08-07",
            "Hinder's controller, not necessarily the controller of the countered spell, chooses where " +
                "the countered spell goes."
        )
    }
}

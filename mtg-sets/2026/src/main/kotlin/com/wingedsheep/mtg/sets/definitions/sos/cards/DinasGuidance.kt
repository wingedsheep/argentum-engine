package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dina's Guidance — Secrets of Strixhaven #184
 * {1}{B}{G} · Instant
 *
 * Search your library for a creature card, reveal it, put it into your hand or graveyard, then * shuffle.
 *
 * A search tutor whose found card lands in a *player-chosen* zone (hand or graveyard). Modeled as
 * an atomic gather → choose → reveal → split-move pipeline:
 *   1. gather the creature cards in your library,
 *   2. `chooseUpTo(1)` — the search itself (a search may always fail to find, CR 701.19c),
 *   3. reveal the found card,
 *   4. a binary split over the found card — selecting it sends it to your **hand**, leaving it
 *      sends it to your **graveyard** (the "hand or graveyard" choice, with the card already
 *      revealed so the decision is made after seeing it),
 *   5. shuffle.
 * No new SDK surface: the zone choice is expressed with the existing split-selection primitive.
 */
val DinasGuidance = card("Dina's Guidance") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "Search your library for a creature card, reveal it, put it into your hand or " +
        "graveyard, then shuffle."

    spell {
        effect = Effects.Pipeline {
            val pool = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Creature),
                search = true
            )
            val found = chooseUpTo(
                1,
                from = pool,
                prompt = "Search your library for a creature card",
            )
            reveal(found)
            val placed = chooseUpToSplit(
                1,
                from = found,
                prompt = "Put the creature card into your hand or graveyard",
                selectedLabel = "Put into your hand",
                remainderLabel = "Put into your graveyard",
            )
            move(placed.selected, CardDestination.ToZone(Zone.HAND))
            move(placed.remainder, CardDestination.ToZone(Zone.GRAVEYARD))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "Manuel Castañón"
        flavorText = "\"This last step is very important. You'll need to know if the friend you're searching for is alive or dead.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/7/775c1e50-08a4-413f-ab0f-f1c2a79cfe94.jpg?1775938273"
    }
}

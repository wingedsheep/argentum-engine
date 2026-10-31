package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Emergent Ultimatum
 * {B}{B}{G}{G}{G}{U}{U}
 * Sorcery
 *
 * Search your library for up to three monocolored cards with different names and exile them.
 * An opponent chooses one of those cards. Shuffle that card into your library. You may cast the
 * other cards without paying their mana costs. Exile Emergent Ultimatum.
 *
 * The Gifts Ungiven / Elemental Teachings pipeline ending in the Villainous Wealth free-cast step:
 *  1. Search for up to three monocolored cards under `OnePerCardName`, and exile what was found
 *     (tracked, so only cards that actually reached exile go on).
 *  2. An opponent ([Chooser.Opponent]) picks exactly one; it's shuffled back into the library.
 *     Finding a single card means that card is the one shuffled away (ruling).
 *  3. The rest may be cast for free *during resolution* (ruling: you can't wait); any card not
 *     cast — including a monocolored land such as Dryad Arbor, which can't be cast — stays exiled.
 *  4. `selfExile()` — the sorcery goes to exile instead of the graveyard.
 */
val EmergentUltimatum = card("Emergent Ultimatum") {
    manaCost = "{B}{B}{G}{G}{G}{U}{U}"
    colorIdentity = "BGU"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to three monocolored cards with different names and " +
        "exile them. An opponent chooses one of those cards. Shuffle that card into your library. " +
        "You may cast the other cards without paying their mana costs. Exile Emergent Ultimatum."

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any.monocolored()),
                search = true
            )
            val found = chooseUpTo(
                3,
                from = searchable,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Search for up to three monocolored cards with different names"
            )
            val exiled = moveTracked(found, CardDestination.ToZone(Zone.EXILE))
            val (toLibrary, castable) = chooseExactlySplit(
                1,
                from = exiled,
                chooser = Chooser.Opponent,
                selectedLabel = "Shuffle into library",
                remainderLabel = "May be cast for free",
                prompt = "Choose one of the exiled cards. It's shuffled into its owner's library; " +
                    "they may cast the others without paying their mana costs."
            )
            move(toLibrary, CardDestination.ToZone(Zone.LIBRARY))
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
            run(Effects.CastAnyNumberFromCollectionWithoutPayingCost(castable))
        }
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a6a52ab-6d38-4429-9969-90064e615152.jpg?1783931025"
        ruling("2025-04-04", "A monocolored card is exactly one of white, blue, black, red, or green and no other colors. A colorless card isn't monocolored.")
        ruling("2025-04-04", "If you find only one card, you'll shuffle it into your library and exile Emergent Ultimatum without casting anything.")
        ruling("2025-04-04", "If you wish to cast any exiled cards, you do so as part of the resolution of Emergent Ultimatum. You can't wait to cast them later. Timing permissions based on a card's type are ignored.")
        ruling("2025-04-04", "Emergent Ultimatum is still on the stack while you cast the cards you found, so those spells could target it. However, once it's exiled, it will no longer be a legal target.")
        ruling("2025-04-04", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs, such as kicker costs. If the spell has any mandatory additional costs, those must be paid to cast the spell.")
        ruling("2025-04-04", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}

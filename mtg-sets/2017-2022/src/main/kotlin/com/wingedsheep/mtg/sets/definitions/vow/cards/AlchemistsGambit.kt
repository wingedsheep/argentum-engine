package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Alchemist's Gambit
 * {1}{R}{R}
 * Sorcery
 * Cleave {4}{U}{U}{R} (You may cast this spell for its cleave cost. If you do, remove the words
 * in square brackets.)
 * Take an extra turn after this one. During that turn, damage can't be prevented. [At the
 * beginning of that turn's end step, you lose the game.]
 * Exile Alchemist's Gambit.
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. Only one
 * bracket span here — the drawback clause `[At the beginning of that turn's end step, you lose the
 * game.]` — so paying the (much steeper) cleave cost buys the extra turn *without* the downside.
 *
 * Two moving parts, neither of which targets, so the two modes differ only in their effect:
 *
 *  - **Extra turn, with its riders.** `TakeExtraTurn(loseAtEndStep = true, damageCantBePrevented =
 *    true)` grants the extra turn and records both "during that turn" clauses on it: damage can't be
 *    prevented for that whole turn, and you lose at its end step. The cleaved effect drops only the
 *    lose clause, which is exactly the brackets-removed version. Both clauses are bound to the extra
 *    turn itself, so if it is skipped as it would begin (Ugin's Nexus, Trouble in Pairs) neither
 *    reaches a later turn.
 *  - **"Exile Alchemist's Gambit."** Also outside the brackets; `selfExile()` routes the spell to
 *    exile instead of the graveyard on resolution in both modes.
 */
val AlchemistsGambit = card("Alchemist's Gambit") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "RU"
    typeLine = "Sorcery"
    oracleText = "Cleave {4}{U}{U}{R} (You may cast this spell for its cleave cost. If you do, " +
        "remove the words in square brackets.)\nTake an extra turn after this one. During that " +
        "turn, damage can't be prevented. [At the beginning of that turn's end step, you lose the " +
        "game.]\nExile Alchemist's Gambit."

    keywordAbility(KeywordAbility.cleave("{4}{U}{U}{R}"))

    spell {
        // Printed (brackets present): extra turn, no prevention, lose at that turn's end step.
        effect = Effects.TakeExtraTurn(loseAtEndStep = true, damageCantBePrevented = true)

        // Cleaved (brackets removed): extra turn, no prevention, but no lose-the-game drawback.
        cleaveEffect = Effects.TakeExtraTurn(damageCantBePrevented = true)

        // "Exile Alchemist's Gambit." — outside the brackets, so it applies to both modes.
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "140"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8fbf5d3-4677-4bf0-891f-57d6dcddaff7.jpg?1782703091"
    }
}

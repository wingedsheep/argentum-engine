package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerTiming
import com.wingedsheep.sdk.scripting.effects.TakeExtraTurnEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

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
 * Three moving parts, none of which are targets, so the two modes differ only in their effect:
 *
 *  - **Extra turn (+ lose clause).** `TakeExtraTurnEffect(loseAtEndStep = true)` both grants the
 *    extra turn and schedules the "you lose the game at that turn's end step" drawback; the cleaved
 *    effect uses `loseAtEndStep = false`, which is exactly the brackets-removed version.
 *  - **"During that turn, damage can't be prevented."** This is scoped to the *extra* turn, not the
 *    turn the spell resolves on, and the engine clears `damageCantBePreventedThisTurn` at every turn
 *    boundary — so it can't be applied at resolution. Instead a delayed trigger
 *    (`timing = NEXT_TURN`, `fireOnPlayer = You`) fires at the beginning of the extra turn's upkeep
 *    and sets the flag for that turn. This clause is *outside* the brackets, so both modes carry it.
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
        // Delayed trigger shared by both modes: at the start of the extra turn (the controller's
        // next turn), make damage impossible to prevent for that whole turn.
        val damageCantBePrevented = CreateDelayedTriggerEffect(
            step = Step.UPKEEP,
            effect = Effects.DamageCantBePreventedThisTurn(),
            fireOnPlayer = EffectTarget.PlayerRef(Player.You),
            timing = DelayedTriggerTiming.NEXT_TURN,
        )

        // Printed (brackets present): extra turn, no prevention, lose at that turn's end step.
        effect = Effects.Composite(
            TakeExtraTurnEffect(loseAtEndStep = true),
            damageCantBePrevented,
        )

        // Cleaved (brackets removed): extra turn, no prevention, but no lose-the-game drawback.
        cleaveEffect = Effects.Composite(
            TakeExtraTurnEffect(loseAtEndStep = false),
            damageCantBePrevented,
        )

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

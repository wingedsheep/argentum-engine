package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.LookAudience
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hostile Negotiations
 * {3}{B}
 * Instant
 * Exile the top three cards of your library in a face-down pile, then exile the top three cards
 * of your library in another face-down pile. Look at the cards in each pile, then turn a pile of
 * your choice face up. An opponent chooses one of those piles. Put that pile into your hand and
 * the other into your graveyard. You lose 3 life.
 *
 * A "divvy" (CR 700.3) whose piles are fixed by library position rather than chosen — the
 * sibling of Curator of Destinies, where you pick the split. Composed from pipeline steps; the
 * hidden information is masking, not a bespoke decision type:
 *  1. Exile the top three face down ([FaceDownMode.HIDDEN]), then the next three the same way.
 *     `lookableInExile` grants *you* the look ("Look at the cards in each pile") while the cards
 *     stay hidden from the opponent; `LookAudience.None` keeps the gather itself from adding a
 *     separate library look.
 *  2. You choose which pile to turn face up; that pile is re-gathered with `revealed = true`, which
 *     makes it public knowledge. The other pile stays opaque to the opponent.
 *  3. An opponent ([Chooser.Opponent] — with several opponents you name which one, per the
 *     Curator ruling shape) chooses a pile; it goes to your hand and the other to your graveyard.
 *  4. You lose 3 life — unconditionally, even when the library was empty.
 *
 * Short library (ruling): the second pile is smaller or empty, and an empty pile can still be
 * turned face up or chosen. Only when nothing at all was exiled is the pile ceremony skipped.
 */
val HostileNegotiations = card("Hostile Negotiations") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile the top three cards of your library in a face-down pile, then exile the top " +
        "three cards of your library in another face-down pile. Look at the cards in each pile, then " +
        "turn a pile of your choice face up. An opponent chooses one of those piles. Put that pile " +
        "into your hand and the other into your graveyard. You lose 3 life."

    spell {
        effect = Effects.Pipeline {
            // "Exile the top three cards of your library in a face-down pile, ..."
            val firstTop = gather(CardSource.TopOfLibrary(3), lookAudience = LookAudience.None)
            val firstPile = moveTracked(
                firstTop,
                CardDestination.ToZone(Zone.EXILE),
                faceDown = FaceDownMode.HIDDEN,
                lookableInExile = true
            )
            // "... then exile the top three cards of your library in another face-down pile."
            val secondTop = gather(CardSource.TopOfLibrary(3), lookAudience = LookAudience.None)
            val secondPile = moveTracked(
                secondTop,
                CardDestination.ToZone(Zone.EXILE),
                faceDown = FaceDownMode.HIDDEN,
                lookableInExile = true
            )
            ifNotEmpty(firstPile) {
                // "Look at the cards in each pile, then turn a pile of your choice face up."
                val flip = choosePile(
                    pileA = firstPile,
                    pileB = secondPile,
                    pileALabel = "First pile",
                    pileBLabel = "Second pile",
                    chooser = Chooser.Controller,
                    prompt = "Choose a pile to turn face up. Your opponent will then choose which " +
                        "pile goes to your hand; the other goes to your graveyard."
                )
                val faceUp = gather(flip.chosen.asSource, revealed = true)
                // "An opponent chooses one of those piles."
                val picked = choosePile(
                    pileA = faceUp,
                    pileB = flip.other,
                    pileALabel = "Face-up pile",
                    pileBLabel = "Face-down pile",
                    chooser = Chooser.Opponent,
                    prompt = "Choose a pile. That pile goes to your opponent's hand; the other goes " +
                        "to their graveyard."
                )
                // "Put that pile into your hand and the other into your graveyard."
                toHand(picked.chosen)
                toGraveyard(picked.other)
            }
        } then Effects.LoseLife(3, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "David Auden Nash"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f827a9a0-7b72-49e2-85fa-adcce1e8fdda.jpg?1783920085"
        ruling(
            "2022-10-14",
            "If you have fewer than six cards in your library, the second pile will be smaller than " +
                "the first. If you have fewer than four cards in your library, only the first pile " +
                "will have cards in it at all, and the second pile will be empty. You may choose to " +
                "turn the empty pile face up, but your opponent will probably choose to give you the " +
                "empty pile regardless, unless they're feeling especially generous."
        )
    }
}

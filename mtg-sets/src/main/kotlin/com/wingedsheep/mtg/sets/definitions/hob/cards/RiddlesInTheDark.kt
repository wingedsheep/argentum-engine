package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Riddles in the Dark
 * {2}{U}
 * Instant
 * Look at the top four cards of your library and separate them into a face-down pile and a
 * face-up pile. An opponent chooses one of the piles. Put that pile into your hand and the other
 * into your graveyard.
 *
 * This uses the existing concealed-pile pipeline shape from Curator of Destinies. The caster sees
 * all four cards while splitting, only the selected face-up pile is revealed, and the opponent
 * chooses by label while the face-down pile remains opaque.
 */
val RiddlesInTheDark = card("Riddles in the Dark") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Look at the top four cards of your library and separate them into a face-down " +
        "pile and a face-up pile. An opponent chooses one of the piles. Put that pile into your " +
        "hand and the other into your graveyard."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(4)), name = "looked")
            ifNotEmpty(looked) {
                val piles = chooseAnyNumberSplit(
                    from = looked,
                    chooser = Chooser.Controller,
                    prompt = "Separate the top four cards into a face-up pile and a face-down pile. " +
                        "The cards you select are placed face up; the rest stay face down.",
                    selectedLabel = "Face-up pile",
                    remainderLabel = "Face-down pile",
                    showAllCards = true,
                    alwaysPrompt = true,
                    name = "faceUp",
                    remainderName = "faceDown"
                )
                val faceUp = gather(piles.selected.asSource, revealed = true, name = "faceUpRevealed")
                val picked = choosePile(
                    pileA = faceUp,
                    pileB = piles.remainder,
                    pileALabel = "Face-up pile",
                    pileBLabel = "Face-down pile",
                    chooser = Chooser.Opponent,
                    prompt = "Choose a pile. That pile goes to your opponent's hand; the other goes " +
                        "to their graveyard."
                )
                toHand(picked.chosen)
                toGraveyard(picked.other)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Lorenzo Mastroianni"
        flavorText = "\"What have I got in my pocket?\"\n—Bilbo"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6129286-7437-4ba4-be55-586a22cd67ca.jpg?1783902787"
        ruling(
            "2026-06-29",
            "You may split the cards into one pile of four and one pile of zero. Either pile may " +
                "be the face-up pile."
        )
        ruling(
            "2026-06-29",
            "You don't have to reveal the cards in the face-down pile if you put it into your hand."
        )
    }
}

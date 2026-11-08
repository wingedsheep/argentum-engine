package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.LookAudience

/**
 * Fortune's Favor
 * {3}{U}
 * Instant
 * Target opponent looks at the top four cards of your library and separates them into a face-down
 * pile and a face-up pile. Put one pile into your hand and the other into your graveyard.
 *
 * The face-down/face-up divvy (CR 700.3) shared with Sauron's Ransom and Atris, Oracle of
 * Half-Truths: the targeted opponent looks ([LookAudience.None] keeps the cards hidden from you) and
 * splits them with [Chooser.TargetPlayer] — `chooseAnyNumberSplit` allows an empty pile either way
 * (ruling). The face-up pile is re-gathered `revealed = true` so it becomes public; the face-down
 * pile is never revealed to you (you see only its size) until it reaches its new zone. You choose
 * which pile goes to your hand; the other goes to your graveyard.
 */
val FortunesFavor = card("Fortune's Favor") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target opponent looks at the top four cards of your library and separates them into a " +
        "face-down pile and a face-up pile. Put one pile into your hand and the other into your graveyard."

    spell {
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            // The targeted opponent looks at the top four; you don't see them.
            val looked = gather(CardSource.TopOfLibrary(4), lookAudience = LookAudience.None)
            // They separate them: the cards they select are the face-up pile, the rest face down.
            val (faceUp, faceDown) = chooseAnyNumberSplit(
                from = looked,
                chooser = Chooser.TargetPlayer,
                selectedLabel = "Face-up pile",
                remainderLabel = "Face-down pile",
                prompt = "Separate the top four cards of your opponent's library into a face-up pile and a " +
                    "face-down pile. The cards you select are placed face up; the rest stay face down.",
                alwaysPrompt = true
            )
            // The face-up pile becomes public; the face-down pile is never revealed.
            val faceUpCards = gather(faceUp.asSource, revealed = true)
            // You choose which pile goes to your hand; the other goes to your graveyard.
            val (keepPile, otherPile) = choosePile(
                faceUpCards,
                faceDown,
                pileALabel = "Face-up pile",
                pileBLabel = "Face-down pile",
                chooser = Chooser.Controller,
                prompt = "Choose which pile goes to your hand; the other goes to your graveyard."
            )
            toHand(keepPile)
            toGraveyard(otherPile)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Yohann Schepacz"
        flavorText = "Uncertainty has its own allure."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a67dd08-0064-4750-adf5-07bacb0296eb.jpg?1783937499"
        ruling(
            "2016-07-13",
            "You choose which pile to put into your hand and which to put into your graveyard. You can't look " +
                "at the face-down pile until after it's moving to its new zone."
        )
        ruling(
            "2016-07-13",
            "A pile can have no cards in it. In this case, you'll choose whether to put all the cards into your " +
                "hand or into your graveyard."
        )
        ruling(
            "2016-07-13",
            "In a multiplayer game, the targeted opponent may choose what information to share with others if " +
                "that player wants advice on how to split the cards, but that player doesn't have to share any " +
                "information at all."
        )
    }
}

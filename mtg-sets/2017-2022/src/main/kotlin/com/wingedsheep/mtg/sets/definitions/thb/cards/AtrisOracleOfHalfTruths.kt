package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.LookAudience

/**
 * Atris, Oracle of Half-Truths
 * {2}{U}{B}
 * Legendary Creature — Human Advisor
 * 3/2
 * Menace
 * When Atris enters, target opponent looks at the top three cards of your library and separates
 * them into a face-down pile and a face-up pile. Put one pile into your hand and the other into
 * your graveyard.
 *
 * Sauron's Ransom's divvy (CR 700.3) as an enters trigger over three cards: the targeted opponent
 * looks ([LookAudience.None] keeps them hidden from you) and splits them with
 * [Chooser.TargetPlayer] — `chooseAnyNumberSplit` allows the 3/0 split either way (ruling); the
 * face-up pile is re-gathered `revealed = true` so it becomes public, the face-down pile stays
 * hidden (and isn't revealed if it goes to your hand — ruling); you pick the pile for your hand.
 */
val AtrisOracleOfHalfTruths = card("Atris, Oracle of Half-Truths") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Advisor"
    power = 3
    toughness = 2
    oracleText = "Menace\n" +
        "When Atris enters, target opponent looks at the top three cards of your library and separates " +
        "them into a face-down pile and a face-up pile. Put one pile into your hand and the other into " +
        "your graveyard."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            // The targeted opponent looks at the top three; you don't see them.
            val looked = gather(CardSource.TopOfLibrary(3), lookAudience = LookAudience.None)
            // They separate them: the cards they select are the face-up pile, the rest face down.
            val (faceUp, faceDown) = chooseAnyNumberSplit(
                from = looked,
                chooser = Chooser.TargetPlayer,
                selectedLabel = "Face-up pile",
                remainderLabel = "Face-down pile",
                prompt = "Separate the top three cards of your opponent's library into a face-up pile and a " +
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
        description = "When Atris enters, target opponent looks at the top three cards of your library and " +
            "separates them into a face-down pile and a face-up pile. Put one pile into your hand and the " +
            "other into your graveyard."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Bastien L. Deharme"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db6c91ec-df14-460f-967c-f182562fe7d8.jpg?1783931525"
        ruling(
            "2020-01-24",
            "You choose just one target opponent to look at the top three cards of your library and put them " +
                "into piles. Other players can't look, but they may attempt to offer advice without knowing what " +
                "cards are there. The player who looks may also tell other players truths about those cards, or " +
                "half-truths, or outright lies."
        )
        ruling("2020-01-24", "You don't have to reveal the cards in the face-down pile if you put it into your hand.")
        ruling(
            "2020-01-24",
            "The opponent may split the cards into one pile of three and one pile of zero. The pile of three " +
                "cards could be the face-up pile or the face-down pile."
        )
    }
}

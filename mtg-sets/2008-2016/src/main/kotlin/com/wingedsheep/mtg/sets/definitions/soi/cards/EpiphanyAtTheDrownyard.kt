package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Epiphany at the Drownyard
 * {X}{U}
 * Instant
 * Reveal the top X plus one cards of your library and separate them into two piles. An
 * opponent chooses one of those piles. Put that pile into your hand and the other into your
 * graveyard.
 *
 * The reverse "divvy" (CR 700.3) of Fact or Fiction: *you* separate, an opponent chooses. Composed
 * from the same pile primitives as Death or Glory — Gather (top X+1, revealed) →
 * SelectFromCollection (you split; empty piles legal) → ChoosePile (an opponent picks the pile that
 * goes to your hand) → MoveCollection (chosen pile to hand, the other to graveyard).
 */
val EpiphanyAtTheDrownyard = card("Epiphany at the Drownyard") {
    manaCost = "{X}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Reveal the top X plus one cards of your library and separate them into two piles. An opponent chooses one of those piles. Put that pile into your hand and the other into your graveyard."

    spell {
        effect = Effects.Pipeline {
            // 1. Reveal the top X plus one cards of your library.
            val revealed = gather(
                CardSource.TopOfLibrary(DynamicAmounts.xValue() + 1),
                revealed = true
            )
            // 2. You separate them into two piles (either may be empty).
            val (pileA, pileB) = chooseAnyNumberSplit(
                from = revealed,
                chooser = Chooser.Controller,
                selectedLabel = "Pile 1",
                remainderLabel = "Pile 2",
                prompt = "Separate the revealed cards into two piles. The cards you select form Pile 1; the rest form Pile 2.",
                alwaysPrompt = true
            )
            // 3. An opponent chooses one pile; that pile goes to your hand.
            val (toHandPile, toGraveyardPile) = choosePile(
                pileA,
                pileB,
                pileALabel = "Pile 1",
                pileBLabel = "Pile 2",
                chooser = Chooser.Opponent,
                prompt = "Choose which pile goes to your opponent's hand; the other goes to their graveyard."
            )
            // 4. Put that pile into your hand and the other into your graveyard.
            toHand(toHandPile)
            toGraveyard(toGraveyardPile)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "Titus Lunter"
        flavorText = "\"Is this what you found, Tamiyo?\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64b44364-8f83-4dcf-8458-0fd54a698524.jpg?1783937800"
        ruling("2016-04-08", "X can be 0, and piles can be empty. If X is 0, you'll reveal one card and one pile will be empty. Your opponent may choose the empty pile to put into your hand.")
    }
}

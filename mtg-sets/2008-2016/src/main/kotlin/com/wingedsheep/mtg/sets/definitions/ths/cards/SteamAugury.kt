package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Steam Augury
 * {2}{U}{R}
 * Instant
 * Reveal the top five cards of your library and separate them into two piles. An opponent
 * chooses one of those piles. Put that pile into your hand and the other into your graveyard.
 *
 * The reverse "divvy" (CR 700.3) of Fact or Fiction — *you* separate, an opponent chooses —
 * composed exactly like Epiphany at the Drownyard with a fixed count of five: Gather (top five,
 * revealed) → SelectFromCollection (you split; empty piles legal) → ChoosePile (an opponent picks
 * the pile that goes to your hand) → MoveCollection.
 */
val SteamAugury = card("Steam Augury") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Reveal the top five cards of your library and separate them into two piles. An opponent chooses one of those piles. Put that pile into your hand and the other into your graveyard."

    spell {
        effect = Effects.Pipeline {
            // 1. Reveal the top five cards of your library.
            val revealed = gather(CardSource.TopOfLibrary(5), revealed = true)
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
        collectorNumber = "205"
        artist = "Dave Kendall"
        flavorText = "Keranos is a fickle god, delivering punishment as readily as prophecy."
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6eb69801-b574-4b5f-82e7-8fd34cdadf74.jpg?1783939724"
    }
}

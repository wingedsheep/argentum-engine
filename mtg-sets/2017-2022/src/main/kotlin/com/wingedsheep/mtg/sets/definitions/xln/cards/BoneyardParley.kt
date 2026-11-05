package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Boneyard Parley
 * {5}{B}{B}
 * Sorcery
 * Exile up to five target creature cards from graveyards. An opponent separates those cards into
 * two piles. Put all cards from the pile of your choice onto the battlefield under your control
 * and the rest into their owners' graveyards.
 *
 * A divvy (Fact or Fiction's shape) over targeted graveyard cards: the still-legal targets are
 * exiled (`moveTracked`, so only cards that actually reached exile are split), an opponent
 * splits them with [Chooser.Opponent] (chosen at resolution in multiplayer, not targeted —
 * ruling), either pile may be empty, you pick the pile that enters under your control, and the
 * other pile goes to each card's owner's graveyard (graveyard moves always route to the owner).
 */
val BoneyardParley = card("Boneyard Parley") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Exile up to five target creature cards from graveyards. An opponent separates those cards " +
        "into two piles. Put all cards from the pile of your choice onto the battlefield under your control " +
        "and the rest into their owners' graveyards."

    spell {
        targets(TargetFilter.CreatureInGraveyard, count = 5, optional = true)
        effect = Effects.Pipeline {
            val targeted = gather(CardSource.ChosenTargets)
            val exiled = moveTracked(targeted, CardDestination.ToZone(Zone.EXILE))
            // An opponent separates the exiled cards: the cards they select form Pile 1.
            val (pileOne, pileTwo) = chooseAnyNumberSplit(
                from = exiled,
                chooser = Chooser.Opponent,
                selectedLabel = "Pile 1",
                remainderLabel = "Pile 2",
                prompt = "Separate the exiled creature cards into two piles. The cards you select form Pile 1; " +
                    "the rest form Pile 2.",
                alwaysPrompt = true
            )
            // You choose the pile that enters the battlefield under your control.
            val (battlefieldPile, graveyardPile) = choosePile(
                pileOne,
                pileTwo,
                pileALabel = "Pile 1",
                pileBLabel = "Pile 2",
                chooser = Chooser.Controller,
                prompt = "Choose which pile enters the battlefield under your control; the other goes to " +
                    "its owners' graveyards."
            )
            move(battlefieldPile, CardDestination.ToZone(Zone.BATTLEFIELD))
            toGraveyard(graveyardPile)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "94"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35989b1e-9bad-4f8d-a559-bf14d1342aa3.jpg?1783935765"
        ruling("2017-09-29", "The creature cards may come from different graveyards.")
        ruling(
            "2017-09-29",
            "You (not your opponent) choose which pile to put onto the battlefield and which to return to graveyards."
        )
        ruling(
            "2017-09-29",
            "A pile can have no cards in it. In this case, you'll choose whether to put all the exiled cards onto " +
                "the battlefield or into graveyards."
        )
        ruling(
            "2017-09-29",
            "In multiplayer games, you choose an opponent to separate the cards when the ability resolves. This " +
                "doesn't target that opponent. Because the cards are revealed, all players may see the cards and " +
                "offer opinions."
        )
    }
}

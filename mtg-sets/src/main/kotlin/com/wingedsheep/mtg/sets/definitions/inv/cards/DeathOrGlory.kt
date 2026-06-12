package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects

/**
 * Death or Glory
 * {4}{W}
 * Sorcery
 * Separate all creature cards in your graveyard into two piles. Exile the pile
 * of an opponent's choice and return the other to the battlefield.
 *
 * A "divvy" (CR 700.3) over the graveyard: you partition your creature cards,
 * an opponent chooses one pile to be exiled, and the rest return. Composed from
 * the same pile primitives as Do or Die / Fact or Fiction — Gather (graveyard) →
 * SelectFromCollection (you split) → ChoosePile (opponent picks the exiled pile) →
 * MoveCollection (exile chosen, battlefield other).
 */
val DeathOrGlory = card("Death or Glory") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Separate all creature cards in your graveyard into two piles. Exile the pile of an opponent's choice and return the other to the battlefield."

    spell {
        effect = Effects.Pipeline {
            // 1. Gather every creature card in your graveyard.
            val creatures = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.Creature
                ),
                name = "creatures"
            )
            // 2. You separate them into two piles.
            val (pileA, pileB) = chooseAnyNumberSplit(
                from = creatures,
                chooser = Chooser.Controller,
                selectedLabel = "Pile 1",
                remainderLabel = "Pile 2",
                prompt = "Separate your creature cards into two piles. The cards you select form Pile 1; the rest form Pile 2.",
                alwaysPrompt = true,
                name = "pileA",
                remainderName = "pileB"
            )
            // 3. An opponent chooses which pile is exiled.
            val (exiled, returned) = choosePile(
                pileA, pileB,
                pileALabel = "Pile 1",
                pileBLabel = "Pile 2",
                chooser = Chooser.Opponent,
                prompt = "Choose which pile of creature cards is exiled; the other returns to the battlefield.",
                chosenName = "exiled",
                otherName = "returned"
            )
            // 4. Exile the chosen pile.
            move(
                exiled,
                CardDestination.ToZone(Zone.EXILE)
            )
            // 5. Return the other pile to the battlefield under your control.
            move(
                returned,
                CardDestination.ToZone(Zone.BATTLEFIELD)
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Jeff Easley"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81f967c9-b38d-489d-96cc-44a6b1804e10.jpg?1562921230"
    }
}

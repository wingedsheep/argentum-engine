package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wash Out
 * {3}{U}
 * Sorcery
 * Return all permanents of the color of your choice to their owners' hands.
 *
 * Composes the chosen-color pattern (Searing Rays / Coalition Dragon cycle) with the
 * gather→move-to-hand bounce pipeline: [Effects.ChooseColorThen] pauses for the color
 * choice, then [GatherCardsEffect] snapshots every battlefield permanent whose colors
 * include the chosen color (via [CardPredicate.HasChosenColor], read from the resolution
 * context) and [MoveCollectionEffect] returns them to their owners' hands.
 */
val WashOut = card("Wash Out") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return all permanents of the color of your choice to their owners' hands."

    spell {
        effect = Effects.ChooseColorThen(
            then = Effects.Composite(
                GatherCardsEffect(
                    source = CardSource.BattlefieldMatching(
                        filter = GameObjectFilter(
                            cardPredicates = listOf(CardPredicate.HasChosenColor),
                        ),
                        player = Player.Each,
                    ),
                    storeAs = "washOut_gathered",
                ),
                MoveCollectionEffect(
                    from = "washOut_gathered",
                    destination = CardDestination.ToZone(Zone.HAND),
                ),
            ),
            prompt = "Choose a color",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Matthew D. Wilson"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/7719d043-5827-4479-825b-23d9e979ead7.jpg?1562918901"
    }
}

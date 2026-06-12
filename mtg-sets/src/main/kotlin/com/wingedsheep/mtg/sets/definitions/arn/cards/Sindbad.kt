package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sindbad
 * {1}{U}
 * Creature — Human
 * 1/1
 * {T}: Draw a card and reveal it. If it isn't a land card, discard it.
 */
val Sindbad = card("Sindbad") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human"
    power = 1
    toughness = 1
    oracleText = "{T}: Draw a card and reveal it. If it isn't a land card, discard it."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            // Peek at the card that is about to be drawn so the pipeline can branch on
            // it after the draw resolves. The InZone(HAND) re-check below keeps the
            // branch honest: if the draw is replaced (dredge-style) or the library is
            // empty, the peeked card never reaches the hand and nothing is revealed
            // or discarded.
            val toDraw = gather(
                CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                name = "toDraw"
            )
            run(Effects.DrawCards(1))
            val drawn = filter(
                toDraw,
                CollectionFilter.InZone(Zone.HAND),
                name = "drawn"
            )
            reveal(drawn, revealToSelf = false)
            val (keptLand, notLand) = filterSplit(
                drawn,
                CollectionFilter.MatchesFilter(GameObjectFilter.Land),
                name = "keptLand",
                restName = "notLand"
            )
            move(
                notLand,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Discard
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Julie Baroh"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b112a10-ac40-4353-bbdd-e5efd4546330.jpg?1562917732"
    }
}

package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Cache Grab
 * {1}{G}
 * Instant
 *
 * Mill four cards. You may put a permanent card from among the cards milled this way
 * into your hand. If you control a Squirrel or returned a Squirrel card to your hand
 * this way, create a Food token.
 */
val CacheGrab = card("Cache Grab") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Mill four cards. You may put a permanent card from among the cards milled this way into your hand. If you control a Squirrel or returned a Squirrel card to your hand this way, create a Food token."

    val squirrelFilter = GameObjectFilter.Any.withSubtype("Squirrel")

    spell {
        effect = Effects.Pipeline {
            // Mill 4: gather top 4, move to graveyard
            val milled = gather(CardSource.TopOfLibrary(DynamicAmount.Fixed(4)), name = "milled")
            move(milled, CardDestination.ToZone(Zone.GRAVEYARD))
            // You may put a permanent card from among the milled cards into your hand
            val selected = chooseUpTo(
                1, from = milled,
                filter = GameObjectFilter.Permanent,
                showAllCards = true,
                prompt = "You may put a permanent card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard",
                name = "selected"
            )
            move(selected, CardDestination.ToZone(Zone.HAND))
            // If you control a Squirrel or returned a Squirrel card, create a Food token
            run(
                ConditionalEffect(
                    condition = Conditions.Any(
                        Conditions.ControlCreatureOfType(com.wingedsheep.sdk.core.Subtype("Squirrel")),
                        Conditions.CollectionContainsMatch("selected", squirrelFilter)
                    ),
                    effect = Effects.CreateFood()
                )
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Loic Canavaggia"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfd977dc-a7c3-4d0a-aca7-b25bd154e963.jpg?1721426785"
    }
}

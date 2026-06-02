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
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
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
        effect = Effects.Composite(
            listOf(
                // Mill 4: gather top 4, move to graveyard
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(4)),
                    storeAs = "milled"
                ),
                MoveCollectionEffect(
                    from = "milled",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                ),
                // You may put a permanent card from among the milled cards into your hand
                SelectFromCollectionEffect(
                    from = "milled",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Permanent,
                    storeSelected = "selected",
                    showAllCards = true,
                    prompt = "You may put a permanent card into your hand",
                    selectedLabel = "Put in hand",
                    remainderLabel = "Leave in graveyard"
                ),
                MoveCollectionEffect(
                    from = "selected",
                    destination = CardDestination.ToZone(Zone.HAND)
                ),
                // If you control a Squirrel or returned a Squirrel card, create a Food token
                ConditionalEffect(
                    condition = Conditions.Any(
                        Conditions.ControlCreatureOfType(com.wingedsheep.sdk.core.Subtype("Squirrel")),
                        Conditions.CollectionContainsMatch("selected", squirrelFilter)
                    ),
                    effect = Effects.CreateFood()
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Loic Canavaggia"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfd977dc-a7c3-4d0a-aca7-b25bd154e963.jpg?1721426785"
    }
}

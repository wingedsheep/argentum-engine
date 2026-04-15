package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Clifftop Lookout
 * {2}{G}
 * Creature — Frog Scout
 * 1/2
 * Reach
 * When this creature enters, reveal cards from the top of your library until you reveal
 * a land card. Put that card onto the battlefield tapped and the rest on the bottom of
 * your library in a random order.
 */
val ClifftopLookout = card("Clifftop Lookout") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Frog Scout"
    oracleText = "Reach\nWhen this creature enters, reveal cards from the top of your library until you reveal a land card. Put that card onto the battlefield tapped and the rest on the bottom of your library in a random order."
    power = 1
    toughness = 2

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CompositeEffect(listOf(
            GatherUntilMatchEffect(
                filter = GameObjectFilter.Land,
                storeMatch = "revealedLand",
                storeRevealed = "allRevealed"
            ),
            RevealCollectionEffect(from = "allRevealed"),
            // allRevealed includes the matched land, so subtract it before bottoming.
            FilterCollectionEffect(
                from = "allRevealed",
                filter = CollectionFilter.ExcludeOtherCollection("revealedLand"),
                storeMatching = "nonLandRevealed"
            ),
            MoveCollectionEffect(
                from = "revealedLand",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped)
            ),
            MoveCollectionEffect(
                from = "nonLandRevealed",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                order = CardOrder.Random
            )
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "John Thacker"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/662d3bcc-65f3-4c69-8ea1-446870a1193d.jpg?1721426786"
    }
}

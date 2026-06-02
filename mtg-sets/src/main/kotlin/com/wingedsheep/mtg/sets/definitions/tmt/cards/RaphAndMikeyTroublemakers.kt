package com.wingedsheep.mtg.sets.definitions.tmt.cards

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
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Raph & Mikey, Troublemakers
 * {5}{R/G}{R/G}
 * Legendary Creature — Mutant Ninja Turtle
 * 7/7
 *
 * Trample, haste
 * Whenever Raph & Mikey attack, reveal cards from the top of your library
 * until you reveal a creature card. Put that card onto the battlefield
 * tapped and attacking and the rest on the bottom of your library in a
 * random order.
 */
val RaphAndMikeyTroublemakers = card("Raph & Mikey, Troublemakers") {
    manaCost = "{5}{R/G}{R/G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    power = 7
    toughness = 7
    oracleText = "Trample, haste\nWhenever Raph & Mikey attack, reveal cards from the top of your library until you reveal a creature card. Put that card onto the battlefield tapped and attacking and the rest on the bottom of your library in a random order."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = CompositeEffect(listOf(
            GatherUntilMatchEffect(
                filter = GameObjectFilter.Creature,
                storeMatch = "raph_creature",
                storeRevealed = "raph_revealed"
            ),
            RevealCollectionEffect(
                from = "raph_revealed",
                fromZone = Zone.LIBRARY,
                toZone = Zone.BATTLEFIELD
            ),
            MoveCollectionEffect(
                from = "raph_creature",
                destination = CardDestination.ToZone(
                    Zone.BATTLEFIELD,
                    Player.You,
                    ZonePlacement.TappedAndAttacking
                )
            ),
            // Everything revealed minus the creature that hit the battlefield goes to
            // the bottom of the library. Without this split, the creature's entity id
            // is still in "raph_revealed" and the next MoveCollection would pull it
            // off the battlefield right after it landed.
            FilterCollectionEffect(
                from = "raph_revealed",
                filter = CollectionFilter.ExcludeOtherCollection("raph_creature"),
                storeMatching = "raph_rest"
            ),
            MoveCollectionEffect(
                from = "raph_rest",
                destination = CardDestination.ToZone(
                    Zone.LIBRARY,
                    Player.You,
                    ZonePlacement.Bottom
                ),
                order = CardOrder.Random
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "167"
        artist = "Aaron J. Riley"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/8795fba4-0ff3-4c04-a81c-60408608a00c.jpg?1769006366"
    }
}

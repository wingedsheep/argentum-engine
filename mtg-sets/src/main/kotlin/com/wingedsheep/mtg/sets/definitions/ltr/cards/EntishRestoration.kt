package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.LibraryPatterns
import com.wingedsheep.sdk.dsl.Effects

/**
 * Entish Restoration
 * {2}{G}
 * Instant
 *
 * Sacrifice a land. Search your library for up to two basic land cards, put them onto
 * the battlefield tapped, then shuffle. If you control a creature with power 4 or greater,
 * instead search your library for up to three basic land cards, put them onto the
 * battlefield tapped, then shuffle.
 */
val EntishRestoration = card("Entish Restoration") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Sacrifice a land. Search your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle. If you control a creature with power 4 or greater, instead search your library for up to three basic land cards, put them onto the battlefield tapped, then shuffle."

    spell {
        effect = Effects.Composite(
            listOf(
                SacrificeEffect(filter = GameObjectFilter.Land),
                ConditionalEffect(
                    condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
                    effect = LibraryPatterns.searchLibrary(
                        filter = GameObjectFilter.BasicLand,
                        count = 3,
                        destination = SearchDestination.BATTLEFIELD,
                        entersTapped = true
                    ),
                    elseEffect = LibraryPatterns.searchLibrary(
                        filter = GameObjectFilter.BasicLand,
                        count = 2,
                        destination = SearchDestination.BATTLEFIELD,
                        entersTapped = true
                    )
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Calder Moore"
        flavorText = "\"Welcome to the Treegarth of Orthanc!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd4dbf80-187b-40e3-9e0b-526f78d9a34e.jpg?1686969333"
    }
}

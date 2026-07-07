package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Glimpse the Core
 * {1}{G}
 * Sorcery
 * Uncommon — The Lost Caverns of Ixalan #186
 * Artist: Francisco Miyara
 *
 * Choose one —
 * • Search your library for a basic Forest card, put that card onto the battlefield tapped, then shuffle.
 * • Return target Cave card from your graveyard to the battlefield tapped.
 *
 * Mode 0 (no target): Searches library for a card with both the Basic supertype and the Forest
 *   land subtype ([GameObjectFilter.BasicLand.withSubtype("Forest")]). Puts the selected card
 *   directly onto the battlefield tapped, then shuffles (CR 701.23). The player may choose to
 *   find no card ("failing to find", CR 701.23b). Modelled as the Gather → Select → Move
 *   pipeline via [Patterns.Library.searchLibrary].
 *
 * Mode 1 (with target): Returns a Cave land card owned by the controller from their graveyard
 *   to the battlefield tapped. Cave is a land subtype; the target filter restricts to lands with
 *   the Cave subtype in the controller's graveyard ([GameObjectFilter.Land.withSubtype("Cave")
 *   .ownedByYou()], zone = GRAVEYARD). Modelled via [Effects.PutOntoBattlefield] with
 *   tapped = true.
 */
val GlimpseTheCore = card("Glimpse the Core") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Search your library for a basic Forest card, put that card onto the battlefield tapped, then shuffle.\n" +
        "• Return target Cave card from your graveyard to the battlefield tapped."

    spell {
        modal {
            mode("Search your library for a basic Forest card, put that card onto the battlefield tapped, then shuffle") {
                effect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand.withSubtype("Forest"),
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true,
                    shuffleAfter = true
                )
            }
            mode("Return target Cave card from your graveyard to the battlefield tapped") {
                val cave = target(
                    "target Cave card from your graveyard",
                    TargetObject(filter = TargetFilter(GameObjectFilter.Land.withSubtype("Cave").ownedByYou(), zone = Zone.GRAVEYARD))
                )
                effect = Effects.PutOntoBattlefield(cave, tapped = true)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Francisco Miyara"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de7d6aed-3dc8-417d-a190-1660cfc8ee4a.jpg?1782694459"
    }
}

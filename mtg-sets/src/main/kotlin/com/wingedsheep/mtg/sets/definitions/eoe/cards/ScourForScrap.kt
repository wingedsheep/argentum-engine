package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Scour for Scrap
 * {3}{U}
 * Instant
 * Choose one or both —
 * • Search your library for an artifact card, reveal it, put it into your hand, then shuffle.
 * • Return target artifact card from your graveyard to your hand.
 */
val ScourForScrap = card("Scour for Scrap") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one or both —\n" +
        "• Search your library for an artifact card, reveal it, put it into your hand, then shuffle.\n" +
        "• Return target artifact card from your graveyard to your hand."

    spell {
        // Modeled as 3 modes: search only, return only, or both (choose one or both)
        val searchEffect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.Artifact,
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )

        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                searchEffect,
                "Search your library for an artifact card, reveal it, put it into your hand, then shuffle"
            ),
            Mode.withTarget(
                Effects.ReturnToHand(EffectTarget.ContextTarget(0)),
                TargetObject(filter = TargetFilter(GameObjectFilter.Companion.Artifact.ownedByYou(), zone = Zone.GRAVEYARD)),
                "Return target artifact card from your graveyard to your hand"
            ),
            Mode.withTarget(
                Effects.Composite(listOf(
                    searchEffect,
                    Effects.ReturnToHand(EffectTarget.ContextTarget(0))
                )),
                TargetObject(filter = TargetFilter(GameObjectFilter.Companion.Artifact.ownedByYou(), zone = Zone.GRAVEYARD)),
                "Search your library for an artifact card and return target artifact card from your graveyard to your hand"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Filip Burburan"
        flavorText = "The Illvoi hunt for stellar fragments that predate even the Fomori."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/517d1b00-7ec4-489a-ac52-657da24a6379.jpg?1752946845"
    }
}

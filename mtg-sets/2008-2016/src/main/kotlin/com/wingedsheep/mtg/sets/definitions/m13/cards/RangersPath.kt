package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Ranger's Path
 * {3}{G}
 * Sorcery
 * Search your library for up to two Forest cards, put them onto the battlefield tapped, then shuffle.
 *
 * "Forest cards" is any card with the Forest subtype, not just basics (e.g. dual lands with the type).
 */
val RangersPath = card("Ranger's Path") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to two Forest cards, put them onto the battlefield tapped, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land.withSubtype(Subtype.FOREST),
            count = 2,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "186"
        artist = "Tomasz Jedruszek"
        flavorText = "The forest can seem like a dense maze. Those who travel the canopy know otherwise."
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26858a53-1054-407a-b2a2-34a7c4ae0f10.jpg?1783940467"
    }
}

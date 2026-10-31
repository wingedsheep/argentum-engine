package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deathsprout
 * {1}{B}{B}{G}
 * Instant
 * Destroy target creature. Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.
 */
val Deathsprout = card("Deathsprout") {
    manaCost = "{1}{B}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "Destroy target creature. Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Seb McKinnon"
        flavorText = "\"They say nothing lasts forever. I say everything lasts forever, just not in the form you may be accustomed to.\"\n—Vraska"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d615557-aea8-4057-9fbd-d62dd98edc13.jpg?1783933401"
    }
}

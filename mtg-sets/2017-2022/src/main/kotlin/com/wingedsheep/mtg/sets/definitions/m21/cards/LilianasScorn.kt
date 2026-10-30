package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Liliana's Scorn
 * {3}{B}{B}
 * Sorcery
 * Destroy target creature. You may search your library and/or graveyard for a card named Liliana,
 * Death Mage, reveal it, and put it into your hand. If you search your library this way, shuffle.
 *
 * The search runs even when the target survives (indestructible); only an illegal target, which
 * makes the whole spell fizzle, skips it — the 2020-06-23 ruling.
 */
val LilianasScorn = card("Liliana's Scorn") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. You may search your library and/or graveyard for a card named " +
        "Liliana, Death Mage, reveal it, and put it into your hand. If you search your library this way, shuffle."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Liliana, Death Mage"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "329"
        artist = "Josh Hass"
        flavorText = "\"We're done here.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b231f941-4acb-46f2-81ae-16e5a28e65af.jpg?1783930620"
        ruling(
            "2020-06-23",
            "If the target creature is an illegal target by the time Liliana's Scorn tries to resolve, the spell won't resolve. You won't search for Liliana, Death Mage. If the target is legal but not destroyed (most likely because it has indestructible), you do search for Liliana."
        )
    }
}

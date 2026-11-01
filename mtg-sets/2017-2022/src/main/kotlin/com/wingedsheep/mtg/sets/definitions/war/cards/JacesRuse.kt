package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Jace's Ruse — War of the Spark #273 (canonical printing; planeswalker deck)
 * {3}{U}{U} · Sorcery
 *
 * Return up to two target creatures to their owner's hand. You may search your library and/or
 * graveyard for a card named Jace, Arcane Strategist, reveal it, and put it into your hand. If you
 * search your library this way, shuffle.
 *
 * The "up to two" bounce is the optional two-target ForEachTarget shape; the search half is the
 * planeswalker-deck tutor shared with Oko's Hospitality and Liliana's Scorn.
 */
val JacesRuse = card("Jace's Ruse") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return up to two target creatures to their owner's hand. You may search your library " +
        "and/or graveyard for a card named Jace, Arcane Strategist, reveal it, and put it into your hand. " +
        "If you search your library this way, shuffle."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.ReturnToHand(EffectTarget.ContextTarget(0))
        ) then Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Jace, Arcane Strategist"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "273"
        artist = "Clint Cearley"
        flavorText = "\"The Eternals can recognize faces. They just think I look like a blue minotaur.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/3/03e8d2c7-cd6e-48bf-9d54-73f3d6cff0aa.jpg?1783933359"
        ruling(
            "2019-05-03",
            "You may cast Jace's Ruse without choosing any target creatures. You'll just search for Jace, " +
                "Arcane Strategist. However, if you choose any targets and all of those targets become " +
                "illegal before Jace's Ruse resolves, the spell won't resolve and you won't search."
        )
    }
}

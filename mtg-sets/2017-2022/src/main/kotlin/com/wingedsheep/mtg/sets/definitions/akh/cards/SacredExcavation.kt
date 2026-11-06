package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sacred Excavation
 * {3}{U}
 * Sorcery
 * Return up to two target cards with cycling from your graveyard to your hand.
 *
 * "Cards with cycling" is `withCycling()` — typecycling counts too (CR 702.29e).
 */
val SacredExcavation = card("Sacred Excavation") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return up to two target cards with cycling from your graveyard to your hand."

    spell {
        targets(
            TargetFilter(GameObjectFilter.Any.withCycling().ownedByYou(), zone = Zone.GRAVEYARD),
            count = 2,
            optional = true
        )
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Noah Bradley"
        flavorText = "The anointed work the mines without pause, their cartouches instilling them with tireless purpose."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1dc2ab27-0f68-480c-99c9-53243c1ab0ac.jpg?1783936516"
    }
}

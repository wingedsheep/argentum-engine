package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Lay Waste
 * {3}{R}
 * Sorcery
 * Destroy target land.
 * Cycling {2}
 */
val LayWaste = card("Lay Waste") {
    manaCost = "{3}{R}"
    typeLine = "Sorcery"

    spell {
        target = TargetPermanent(filter = TargetFilter.Land)
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true)
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "207"
        artist = "Brian Snoddy"
        flavorText = "\"You built on top of a burial mound? Your fault, not mine.\"\n—Cabal patriarch"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d1a6572-1a65-4461-86d3-7a8e4f9547c2.jpg"
    }
}

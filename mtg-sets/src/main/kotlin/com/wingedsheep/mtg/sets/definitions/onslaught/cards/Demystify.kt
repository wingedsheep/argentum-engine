package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Demystify
 * {W}
 * Instant
 * Destroy target enchantment.
 */
val Demystify = card("Demystify") {
    manaCost = "{W}"
    typeLine = "Instant"

    spell {
        target = TargetPermanent(filter = TargetFilter.Enchantment)
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Greg Staples"
        flavorText = "\"The diffusion of knowledge will eventually undermine all barriers to truth.\"\n—Ixidor, reality sculptor"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0df839f-dc4c-44b0-82c7-cb2037172ac5.jpg?1562944804"
    }
}

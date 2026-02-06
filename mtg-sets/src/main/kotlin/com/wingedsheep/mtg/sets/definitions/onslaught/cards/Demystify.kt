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
        collectorNumber = "31"
        artist = "Greg Staples"
        flavorText = "\"The diffusion of knowledge will eventually undermine all barriers to truth.\"\n—Ixidor, reality sculptor"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68f04348-e106-4031-ad7a-b695c1f8fb77.jpg?1562916811"
    }
}

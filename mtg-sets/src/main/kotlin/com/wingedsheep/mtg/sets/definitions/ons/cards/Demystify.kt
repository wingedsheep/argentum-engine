package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.dsl.Effects

/**
 * Demystify
 * {W}
 * Instant
 * Destroy target enchantment.
 */
val Demystify = card("Demystify") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target enchantment."

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.Enchantment))
        effect = Effects.Move(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Christopher Rush"
        flavorText = "\"The diffusion of knowledge will eventually undermine all barriers to truth.\"\n—Ixidor, reality sculptor"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0df839f-dc4c-44b0-82c7-cb2037172ac5.jpg?1562944804"
    }
}

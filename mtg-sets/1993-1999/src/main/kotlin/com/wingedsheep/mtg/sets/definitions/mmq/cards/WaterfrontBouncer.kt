package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Waterfront Bouncer
 * {1}{U}
 * Creature — Merfolk Spellshaper
 * 1 / 1
 */
val WaterfrontBouncer = card("Waterfront Bouncer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Spellshaper"
    oracleText = "{U}, {T}, Discard a card: Return target creature to its owner's hand."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap, Costs.DiscardCard)
        val t = target("target", Targets.Creature)
        effect = Effects.ReturnToHand(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Paolo Parente"
        flavorText = "Closing time comes earlier to some than to others."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8dbdce9e-94fa-4ed5-9b97-d2026cffe7cb.jpg"
    }
}

package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deny the Divine
 * {2}{U}
 * Instant
 * Counter target creature or enchantment spell. If that spell is countered this way, exile it
 * instead of putting it into its owner's graveyard.
 *
 * Dissipate's [Effects.CounterSpellToExile], narrowed to creature or enchantment spells on the
 * stack (Reject's target shape).
 */
val DenyTheDivine = card("Deny the Divine") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature or enchantment spell. If that spell is countered this way, " +
        "exile it instead of putting it into its owner's graveyard."

    spell {
        target(TargetFilter(GameObjectFilter.Creature or GameObjectFilter.Enchantment, zone = Zone.STACK))
        effect = Effects.CounterSpellToExile()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Yigit Koroglu"
        flavorText = "\"A great wave crashed, and a mighty wind blew out the stars.\"\n—The Cosmogony"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/1200f68a-a8ea-4777-b6b0-de48b2203fd1.jpg?1783931585"
    }
}

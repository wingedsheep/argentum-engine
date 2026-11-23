package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scorchwalker — Gatecrash #104 (canonical printing)
 * {3}{R} · Creature — Elemental · 5/1
 *
 * Bloodrush — {1}{R}{R}, Discard this card: Target attacking creature gets +5/+1 until end of turn.
 */
val Scorchwalker = card("Scorchwalker") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 5
    toughness = 1
    oracleText = "Bloodrush — {1}{R}{R}, Discard this card: Target attacking creature gets +5/+1 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}{R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(5, 1, creature)
        description = "Bloodrush — {1}{R}{R}, Discard this card: Target attacking creature gets +5/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Anthony Palumbo"
        flavorText = "\"Borborygmos's elemental launched an assault, but the Izzet wards held and experiments continue.\"\n—Skyknight report on the Rubblebelt"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14ac6bde-1fef-45f4-b505-80a66b03140a.jpg"
    }
}

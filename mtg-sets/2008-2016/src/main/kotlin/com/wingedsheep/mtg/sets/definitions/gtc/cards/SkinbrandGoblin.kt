package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skinbrand Goblin — Gatecrash #105 (canonical printing)
 * {1}{R} · Creature — Goblin Warrior · 2/1
 *
 * Bloodrush — {R}, Discard this card: Target attacking creature gets +2/+1 until end of turn.
 */
val SkinbrandGoblin = card("Skinbrand Goblin") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 2
    toughness = 1
    oracleText = "Bloodrush — {R}, Discard this card: Target attacking creature gets +2/+1 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(2, 1, creature)
        description = "Bloodrush — {R}, Discard this card: Target attacking creature gets +2/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Marco Nelor"
        flavorText = "\"These Gruul skin-runes—discern whether their patterns evoke streets, leylines, or both. You have four days.\"\n—Niv-Mizzet, to his assistant"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe4f9b6c-3ba9-4f4f-8135-f5236195e507.jpg"
    }
}

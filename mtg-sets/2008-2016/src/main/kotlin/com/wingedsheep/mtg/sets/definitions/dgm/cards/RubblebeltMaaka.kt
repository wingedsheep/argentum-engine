package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rubblebelt Maaka — Dragon's Maze #38 (canonical printing)
 * {3}{R} · Creature — Cat · 3/3
 *
 * Bloodrush — {R}, Discard this card: Target attacking creature gets +3/+3 until end of turn.
 *
 * Bloodrush is an ability word: an activated ability usable only from hand, with discarding the
 * card as part of its cost (the channel shape — Saiba Trespassers).
 */
val RubblebeltMaaka = card("Rubblebelt Maaka") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Cat"
    power = 3
    toughness = 3
    oracleText = "Bloodrush — {R}, Discard this card: Target attacking creature gets +3/+3 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val creature = target(TargetFilter.AttackingCreature)
        effect = Effects.ModifyStats(3, 3, creature)
        description = "Bloodrush — {R}, Discard this card: Target attacking creature gets +3/+3 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Eric Velhagen"
        flavorText = "\"The maaka remain loyal to us in this time of strife. We don't even have to feed them—our enemies do.\"\n—Nedja, Gruul shaman"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc802d62-6559-45b9-ad11-de5887aece2b.jpg"
    }
}

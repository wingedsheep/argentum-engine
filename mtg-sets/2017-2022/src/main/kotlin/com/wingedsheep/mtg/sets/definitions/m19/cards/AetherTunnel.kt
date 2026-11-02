package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Aether Tunnel
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+0 and can't be blocked.
 */
val AetherTunnel = card("Aether Tunnel") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature gets +1/+0 and can't be blocked."
    auraTarget = TargetObject(filter = TargetFilter.Creature)
    staticAbility {
        ability = ModifyStats(1, 0)
    }
    staticAbility {
        // Scoped to the enchanted creature — the default `source()` scope would land on the Aura.
        ability = CantBeBlocked(GroupFilter.attachedCreature())
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Lucas Graciano"
        flavorText = "If you can't find a doorway, make one."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d6178ed-6353-4733-81e1-7b3dc592c3bd.jpg?1783934592"
    }
}

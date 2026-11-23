package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Paralyzing Grasp — Zendikar #58
 * {2}{U} · Enchantment — Aura · Common
 *
 * Enchant creature
 * Enchanted creature doesn't untap during its controller's untap step.
 */
val ParalyzingGrasp = card("Paralyzing Grasp") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature doesn't untap during its controller's untap step."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Izzy"
        flavorText = "The Halimar Sea Caves are both the breeding ground of giant squids and the " +
            "dumping ground of hardened criminals."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4af35801-9280-4ec1-9399-e34501919a8f.jpg?1783942162"
    }
}

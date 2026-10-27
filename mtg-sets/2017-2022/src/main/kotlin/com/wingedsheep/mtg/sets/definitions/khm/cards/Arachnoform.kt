package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.IsAllCreatureTypes
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Arachnoform
 * {1}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2, has reach, and is every creature type.
 *
 * "Is every creature type" is [IsAllCreatureTypes] on the enchanted creature (the Runed Stalactite
 * shape) — it adds every creature type without granting changeling.
 */
val Arachnoform = card("Arachnoform") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2, has reach, and is every creature type."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH)
    }

    staticAbility {
        ability = IsAllCreatureTypes(GroupFilter.attachedCreature())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "159"
        artist = "Ilse Gort"
        flavorText = "Deep within endless webs of clinging silk, the weaver awaits."
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acbe560b-f7b5-4614-91f1-b669a39abc16.jpg?1783928219"
    }
}

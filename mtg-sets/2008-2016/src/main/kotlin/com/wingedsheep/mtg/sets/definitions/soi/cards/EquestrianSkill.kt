package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Equestrian Skill
 * {3}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +3/+3.
 * As long as enchanted creature is a Human, it has trample.
 *
 * The trample grant is gated on the enchanted creature's projected subtype (the Hope Against Hope
 * shape), so it tracks the creature's type continuously.
 */
val EquestrianSkill = card("Equestrian Skill") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +3/+3.\n" +
        "As long as enchanted creature is a Human, it has trample."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(3, 3, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.attachedCreature())
        condition = EnchantedCreatureHasSubtype(Subtype.HUMAN)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "204"
        artist = "Zoltan Boros"
        flavorText = "From the jagged peaks of Stensia, to the tangled woods of Kessig, to the bogs of " +
            "Nephalia, the Gavony Riders prepare for all terrain."
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9af14d1-a304-4744-9d6f-9ff5fcd92ad3.jpg?1783937731"
    }
}

package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Iona's Blessing
 * {3}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2, has vigilance, and can block an additional creature each combat.
 */
val IonasBlessing = card("Iona's Blessing") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2, has vigilance, and can block an additional creature each combat."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CanBlockAdditionalForCreatureGroup(
            count = 1,
            filter = GroupFilter.attachedCreature()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "David Gaillet"
        flavorText = "\"Walk now in my light. When the rest of the world closes its eyes, you will still see.\"\n—Iona"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a30f26f7-5181-4bf1-9084-5563b4996a3c.jpg?1783937926"
        ruling(
            "2016-01-22",
            "The ability to block an additional creature is cumulative. If a creature is enchanted with two Iona's Blessings, it can block three creatures each combat."
        )
    }
}

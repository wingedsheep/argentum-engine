package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Mardu Runemark
 * {2}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2.
 * Enchanted creature has first strike as long as you control a white or black permanent.
 *
 * "You" is the Aura's controller. The colour check reads projected colours, so a permanent made
 * white or black by an effect counts, and the first strike grant switches on and off continuously.
 */
val MarduRunemark = card("Mardu Runemark") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2.\n" +
        "Enchanted creature has first strike as long as you control a white or black permanent."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.attachedCreature()),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withAnyColor(Color.WHITE, Color.BLACK)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/333df10b-7db4-4f8a-a754-e8a630b26cac.jpg?1783938687"
    }
}

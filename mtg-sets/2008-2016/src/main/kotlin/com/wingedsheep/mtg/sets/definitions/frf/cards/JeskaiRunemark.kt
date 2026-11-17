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
 * Jeskai Runemark
 * {2}{U}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2.
 * Enchanted creature has flying as long as you control a red or white permanent.
 *
 * "You" is the Aura's controller. The colour check reads projected colours, so a permanent made
 * red or white by an effect counts, and the flying grant switches on and off continuously.
 */
val JeskaiRunemark = card("Jeskai Runemark") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2.\n" +
        "Enchanted creature has flying as long as you control a red or white permanent."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FLYING, GroupFilter.attachedCreature()),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withAnyColor(Color.RED, Color.WHITE)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2ada6302-326b-4ab1-a9a3-ae4fa6a9001c.jpg?1783938706"
    }
}

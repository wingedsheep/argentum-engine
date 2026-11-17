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
 * Temur Runemark
 * {2}{G}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2.
 * Enchanted creature has trample as long as you control a blue or red permanent.
 *
 * "You" is the Aura's controller. The colour check reads projected colours, so a permanent made
 * blue or red by an effect counts, and the trample grant switches on and off continuously.
 */
val TemurRunemark = card("Temur Runemark") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2.\n" +
        "Enchanted creature has trample as long as you control a blue or red permanent."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.attachedCreature()),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withAnyColor(Color.BLUE, Color.RED)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Ryan Alexander Lee"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/ddd76874-842e-4f4c-b18e-0e441dae59d7.jpg?1783938677"
    }
}

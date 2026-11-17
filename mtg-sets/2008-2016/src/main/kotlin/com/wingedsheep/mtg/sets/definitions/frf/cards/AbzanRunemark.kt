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
 * Abzan Runemark
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2.
 * Enchanted creature has vigilance as long as you control a black or green permanent.
 *
 * "You" is the Aura's controller. The colour check reads projected colours, so a permanent made
 * black or green by an effect counts, and the vigilance grant switches on and off continuously.
 */
val AbzanRunemark = card("Abzan Runemark") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2.\n" +
        "Enchanted creature has vigilance as long as you control a black or green permanent."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.attachedCreature()),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withAnyColor(Color.BLACK, Color.GREEN)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe356261-3126-41f1-ae71-b69ca5641f84.jpg?1783938716"
    }
}

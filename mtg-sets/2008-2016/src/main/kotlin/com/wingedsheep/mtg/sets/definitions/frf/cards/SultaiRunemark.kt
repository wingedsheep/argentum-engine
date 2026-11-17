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
 * Sultai Runemark
 * {2}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2.
 * Enchanted creature has deathtouch as long as you control a green or blue permanent. (Any amount of damage it deals to a creature is enough to destroy it.)
 *
 * "You" is the Aura's controller. The colour check reads projected colours, so a permanent made
 * green or blue by an effect counts, and the deathtouch grant switches on and off continuously.
 */
val SultaiRunemark = card("Sultai Runemark") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2.\n" +
        "Enchanted creature has deathtouch as long as you control a green or blue permanent. (Any amount of damage it deals to a creature is enough to destroy it.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2, GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DEATHTOUCH, GroupFilter.attachedCreature()),
            condition = Conditions.YouControl(GameObjectFilter.Permanent.withAnyColor(Color.GREEN, Color.BLUE)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98d73ae0-de01-4a70-a51c-b990687561a1.jpg?1783938693"
    }
}

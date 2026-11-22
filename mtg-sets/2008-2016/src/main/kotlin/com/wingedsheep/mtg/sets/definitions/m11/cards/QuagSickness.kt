package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Quag Sickness
 * {2}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets -1/-1 for each Swamp you control.
 */
val QuagSickness = card("Quag Sickness") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets -1/-1 for each Swamp you control."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        val negSwamps = -DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Land.withSubtype(Subtype.SWAMP)
        ).count()
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = negSwamps,
            toughnessBonus = negSwamps
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Martina Pilcerova"
        flavorText = "The dread gases didn't kill Farbid. But as he lay in the muck, miserable and helpless, " +
            "watching ghouls and rats advance on him, he wished they had."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21d56d13-b9de-44db-b235-4f7eea60f424.jpg?1783941812"
        ruling(
            "2010-08-15",
            "This ability counts the number of Swamps controlled by Quag Sickness's controller, not the " +
                "enchanted creature's controller (in case they're different players)."
        )
        ruling("2010-08-15", "This bonus is not fixed; it changes as the number of Swamps you control changes.")
        ruling(
            "2010-08-15",
            "The ability cares about lands with the land type Swamp, not necessarily lands named Swamp."
        )
    }
}

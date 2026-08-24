package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Goblin Caves
 * {1}{R}{R}
 * Enchantment — Aura
 * Enchant land
 * As long as enchanted land is a basic Mountain, Goblin creatures get +0/+2.
 *
 * A lord whose switch is somewhere else entirely: the anthem covers every Goblin on the
 * battlefield, but only while the *enchanted land* still reads as a basic Mountain. Both halves are
 * live — a land that becomes a basic Mountain later turns the anthem on, and one that stops being
 * one turns it off, which is why this is a `ConditionalStaticAbility` rather than a check made once
 * when the Aura enters.
 *
 * "Basic Mountain", not "Mountain": a nonbasic land with the Mountain subtype (a dual land, or one
 * a Phantasmal Terrain turned into a Mountain) does not switch it on.
 *
 * The anthem names no controller, so an opponent's Goblins get the toughness too.
 */
val GoblinCaves = card("Goblin Caves") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nAs long as enchanted land is a basic Mountain, Goblin creatures get +0/+2."
    auraTarget = Targets.Land

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 0,
                toughnessBonus = 2,
                filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN)),
            ),
            condition = Conditions.EnchantedPermanentMatches(
                GameObjectFilter.BasicLand.withSubtype(Subtype.MOUNTAIN)
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Drew Tucker"
        flavorText = "The stench of countless generations of unspeakable activities was enough to " +
            "loosen both our footing and our stomachs."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6a415b0-00a2-4a65-8994-4a395c50ae2d.jpg?1783947935"
    }
}

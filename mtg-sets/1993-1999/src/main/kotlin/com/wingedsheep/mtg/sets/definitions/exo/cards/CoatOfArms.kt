package com.wingedsheep.mtg.sets.definitions.exo.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Coat of Arms
 * {5}
 * Artifact
 * Each creature gets +1/+1 for each other creature on the battlefield that shares at least one
 * creature type with it.
 *
 * Alpha Status's per-creature count, applied to every creature: `GrantDynamicStats` evaluates its
 * amount once per affected creature with that creature bound as `EffectTarget.AffectedEntity`, and
 * `excludeSelf` drops the affected creature itself from its own count. Creature types are read from
 * projected state, so a changeling shares a type with every creature that has one. It counts
 * creatures, not shared types.
 */
val CoatOfArms = card("Coat of Arms") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Each creature gets +1/+1 for each other creature on the battlefield that shares at least one " +
        "creature type with it. (For example, if two Goblin Warriors and a Goblin Shaman are on the battlefield, " +
        "each gets +2/+2.)"

    staticAbility {
        val sharedTypeCount = DynamicAmounts.battlefield(
            Player.Each,
            GameObjectFilter.Creature.sharingCreatureTypeWith(EffectTarget.AffectedEntity),
            excludeSelf = true,
        ).count()
        ability = GrantDynamicStats(
            filter = GroupFilter.AllCreatures,
            powerBonus = sharedTypeCount,
            toughnessBonus = sharedTypeCount,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Scott M. Fischer"
        flavorText = "\"Hup, two, three, four,\nDunno how to count no more.\"\n—Mogg march"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e32c939-1d64-4082-bafe-59dfa9c054f6.jpg?1783946501"
        ruling(
            "2009-10-01",
            "Sharing multiple creature types doesn't give an additional bonus. Coat of Arms counts creatures, not " +
                "creature types."
        )
        ruling(
            "2004-10-04",
            "If a creature has more than one creature type, and one of those types matches the creature you are " +
                "calculating for, then count that creature. Only one type needs to match in order to get counted."
        )
    }
}

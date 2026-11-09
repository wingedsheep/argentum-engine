package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Spreading Flames
 * {6}{R}
 * Instant
 * Spreading Flames deals 6 damage divided as you choose among any number of target creatures.
 *
 * Pyrotechnics' shape restricted to creatures: each target must be assigned at least 1 damage,
 * so "any number" caps at six targets, and the division is announced as the spell is cast.
 */
val SpreadingFlames = card("Spreading Flames") {
    manaCost = "{6}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Spreading Flames deals 6 damage divided as you choose among any number of target creatures."

    spell {
        target = TargetObject(filter = TargetFilter.Creature, count = 6, minCount = 1)
        effect = Effects.DividedDamage(total = 6, minTargets = 1, maxTargets = 6)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Chase Stone"
        flavorText = "\"Hang on. I'm just tidying up.\"\n—Chandra Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c655b1dc-8df1-44c8-a963-bd0a671ec7e5.jpg?1783937452"
        ruling(
            "2016-07-13",
            "You announce how the damage will be divided as part of casting Spreading Flames. Each chosen " +
                "target must receive at least 1 damage."
        )
        ruling(
            "2016-07-13",
            "If Spreading Flames has multiple targets, and some, but not all, are illegal targets when " +
                "Spreading Flames tries to resolve, Spreading Flames will still deal damage to the remaining " +
                "legal targets according to the original damage division. If all the targets are illegal, " +
                "the spell won't resolve."
        )
    }
}

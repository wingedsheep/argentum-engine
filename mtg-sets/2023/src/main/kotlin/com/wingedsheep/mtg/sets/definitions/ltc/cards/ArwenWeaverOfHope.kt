package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arwen, Weaver of Hope {1}{G}{G}
 * Legendary Creature — Elf Noble
 * 2/1
 *
 * Each other creature you control enters with a number of additional +1/+1 counters on it equal
 * to Arwen's toughness.
 *
 * An other-only [EntersWithDynamicCounters] (the Grumgully shape). Its count is measured against
 * the entering creature, so "Arwen's toughness" names the replacement's source through
 * [EffectTarget.GrantingSource], read as the creature enters (her current, projected toughness).
 */
val ArwenWeaverOfHope = card("Arwen, Weaver of Hope") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Noble"
    power = 2
    toughness = 1
    oracleText = "Each other creature you control enters with a number of additional +1/+1 " +
        "counters on it equal to Arwen's toughness."

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.toughnessOf(EffectTarget.GrantingSource),
            otherOnly = true,
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Axel Sauerwald"
        flavorText = "From afar, Arwen watched over Aragorn, and in hope, she made for him a great and kingly standard."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d4f4d41-232d-4377-be69-de590b880edb.jpg?1783916027"
        ruling("2023-06-23", "If Arwen, Weaver of Hope and another creature you control enter the battlefield at the same time, Arwen's ability won't cause that creature to enter with additional +1/+1 counters on it.")
        ruling("2023-06-23", "To determine how many additional +1/+1 counters a creature enters the battlefield with, use Arwen, Weaver of Hope's toughness as that creature is entering the battlefield.")
    }
}

package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thirsting Bloodlord (M20 #293)
 * {2}{B}{B}  Creature — Vampire  3/3
 *
 * Other Vampires you control get +1/+1.
 */
val ThirstingBloodlord = card("Thirsting Bloodlord") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    oracleText = "Other Vampires you control get +1/+1."
    power = 3
    toughness = 3

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE).youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "293"
        artist = "Joseph Meehan"
        flavorText = "\"A whole life, full of memories and experience, love and sorrow, drained drop by drop into this bottle. Could any feast be more glorious?\""
        imageUri = "https://cards.scryfall.io/normal/front/5/9/599e8ed3-4341-4373-8871-2768a90d98c1.jpg"
    }
}

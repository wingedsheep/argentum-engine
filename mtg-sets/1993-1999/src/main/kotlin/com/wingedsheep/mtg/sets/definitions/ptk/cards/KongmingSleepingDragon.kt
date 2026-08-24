package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kongming, "Sleeping Dragon"
 * {2}{W}{W}
 * Legendary Creature — Human Advisor
 * 2/2
 * Other creatures you control get +1/+1.
 *
 * The plain lord shape: a [ModifyStats] static over a [GroupFilter] of
 * `GameObjectFilter.Creature.youControl()`. "Other" is the filter's `excludeSelf`, not a separate
 * predicate — Kongming pumps the rest of the team and stays a 2/2 itself.
 */
val KongmingSleepingDragon = card("Kongming, \"Sleeping Dragon\"") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Advisor"
    power = 2
    toughness = 2
    oracleText = "Other creatures you control get +1/+1."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Gao Yan"
        flavorText = "\"Such a lord as this—all virtues' height—Had never been, nor ever was again.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4ae0a05-1870-4f4a-b8a2-bfb5381acacb.jpg"
    }
}

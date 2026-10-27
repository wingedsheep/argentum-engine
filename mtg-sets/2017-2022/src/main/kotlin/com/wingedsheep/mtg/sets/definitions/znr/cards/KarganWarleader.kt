package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kargan Warleader — Zendikar Rising #224
 * {1}{R}{W} · Creature — Human Warrior · 3/3
 *
 * Other Warriors you control get +1/+1.
 *
 * The bare tribal noun "Warriors" is a permanent filter; "Other" is the group's `excludeSelf`.
 */
val KarganWarleader = card("Kargan Warleader") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Warrior"
    oracleText = "Other Warriors you control get +1/+1."
    power = 3
    toughness = 3

    staticAbility {
        ability = ModifyStats(
            1,
            1,
            GroupFilter(GameObjectFilter.Permanent.withSubtype("Warrior").youControl(), excludeSelf = true),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "224"
        artist = "Colin Boyer"
        flavorText = "From rebellion against the Akoum Skyclave to fighting the Eldrazi titans, the dragon-riding " +
            "Kargan tribes have never missed an opportunity to defy the odds."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f921dca-ce93-42d1-87b1-149973558467.jpg"
    }
}

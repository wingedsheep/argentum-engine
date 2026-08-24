package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Marsh Gas
 * {B}
 * Instant
 * All creatures get -2/-0 until end of turn.
 */
val MarshGas = card("Marsh Gas") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "All creatures get -2/-0 until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreatures,
            Effects.ModifyStats(-2, 0, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Douglas Shuler"
        flavorText = "\"Comes right outta th' ground. If ya can smell it, it's too late.\"\n—Keevy Bogsbury"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b80ecb15-258b-4fc9-86e4-c2bf01891606.jpg?1783947938"
    }
}

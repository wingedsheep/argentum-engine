package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Riding the Dilu Horse
 * {2}{G}
 * Sorcery
 * Target creature gets +2/+2 and gains horsemanship. (This effect lasts indefinitely.)
 */
val RidingTheDiluHorse = card("Riding the Dilu Horse") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +2/+2 and gains horsemanship. (It can't be blocked except by creatures with horsemanship. This effect lasts indefinitely.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, t, Duration.Permanent) then
            Effects.GrantKeyword(Keyword.HORSEMANSHIP, t, Duration.Permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "144"
        artist = "Hong Yan"
        flavorText = "\"All men have their appointed time; that's something no horse can change.\"\n—Liu Bei, after being told that the Dilu brings its master ill fortune"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/676fea93-7f39-4fc7-89ab-bb3ea3f15951.jpg?1783946099"
    }
}

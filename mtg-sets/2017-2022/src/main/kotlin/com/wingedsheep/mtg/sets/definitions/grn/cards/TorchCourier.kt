package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Torch Courier
 * {R}
 * Creature — Goblin
 * 1/1
 * Haste
 * Sacrifice this creature: Another target creature gains haste until end of turn.
 */
val TorchCourier = card("Torch Courier") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "Haste\nSacrifice this creature: Another target creature gains haste until end of turn."
    power = 1
    toughness = 1

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.SacrificeSelf
        val creature = target(TargetFilter.OtherCreature)
        effect = Effects.GrantKeyword(Keyword.HASTE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Mark Zug"
        flavorText = "\"Light a torch and deliver this letter\" were his instructions, which he unfortunately reversed."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4c9fc8c-e68f-4636-84b8-877f6ec04b09.jpg"
    }
}

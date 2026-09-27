package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Burr Grafter
 * {3}{G}
 * Creature — Spirit
 * 2/2
 * Sacrifice this creature: Target creature gets +2/+2 until end of turn.
 * Soulshift 3 (When this creature dies, you may return target Spirit card with mana value 3 or less
 * from your graveyard to your hand.)
 *
 * Sacrificing it to its own ability is a death, so the soulshift trigger fires off the cost.
 */
val BurrGrafter = card("Burr Grafter") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "Sacrifice this creature: Target creature gets +2/+2 until end of turn.\n" +
        "Soulshift 3 (When this creature dies, you may return target Spirit card with mana value 3 or less " +
        "from your graveyard to your hand.)"

    activatedAbility {
        cost = Costs.SacrificeSelf
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, t)
    }

    soulshift(3)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/935e52a1-a651-4d88-99a8-7de074a01576.jpg?1783944291"
    }
}

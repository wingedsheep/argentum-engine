package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scuttling Death
 * {4}{B}
 * Creature — Spirit
 * 4/2
 * Sacrifice this creature: Target creature gets -1/-1 until end of turn.
 * Soulshift 4 (When this creature dies, you may return target Spirit card with mana value 4 or less
 * from your graveyard to your hand.)
 */
val ScuttlingDeath = card("Scuttling Death") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 4
    toughness = 2
    oracleText = "Sacrifice this creature: Target creature gets -1/-1 until end of turn.\n" +
        "Soulshift 4 (When this creature dies, you may return target Spirit card with mana value 4 or less " +
        "from your graveyard to your hand.)"

    activatedAbility {
        cost = Costs.SacrificeSelf
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-1, -1, creature)
        description = "Sacrifice this creature: Target creature gets -1/-1 until end of turn."
    }

    soulshift(4)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Thomas M. Baxa"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8d9bf4e-3bb1-4154-b1ff-a3d8a2810c1b.jpg?1783944307"
    }
}

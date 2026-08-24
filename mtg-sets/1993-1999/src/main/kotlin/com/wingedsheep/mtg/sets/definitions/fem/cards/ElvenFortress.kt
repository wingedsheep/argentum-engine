package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Elven Fortress
 * {G}
 * Enchantment
 * {1}{G}: Target blocking creature gets +0/+1 until end of turn.
 */
val ElvenFortress = card("Elven Fortress") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "{1}{G}: Target blocking creature gets +0/+1 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        val t = target("target blocking creature", TargetCreature(filter = TargetFilter.BlockingCreature))
        effect = Effects.ModifyStats(0, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65a"
        artist = "Pete Venters"
        flavorText = "Many Elven Fortresses weren't built by masons and carpenters, but created from the living forest itself."
        imageUri = "https://cards.scryfall.io/normal/front/9/3/9387105d-46d0-4db0-8980-dd0fded15eef.jpg?1783947891"
    }
}

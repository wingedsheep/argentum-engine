package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.GrantSupertype
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Glittering Frost
 * {2}{G}
 * Snow Enchantment — Aura
 * Enchant land
 * Enchanted land is snow.
 * Whenever enchanted land is tapped for mana, its controller adds an additional one mana of any color.
 *
 * The additional mana's source is this snow Aura (CR 106.3), so it is snow mana too.
 */
val GlitteringFrost = card("Glittering Frost") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Snow Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land is snow.\n" +
        "Whenever enchanted land is tapped for mana, its controller adds an additional one mana of any color."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantSupertype("SNOW")
    }

    staticAbility {
        ability = AdditionalManaOnTap(amount = DynamicAmounts.fixed(1), anyColor = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "171"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c60fbc33-6198-4661-967e-cc94f2788e4a.jpg?1783928215"
    }
}

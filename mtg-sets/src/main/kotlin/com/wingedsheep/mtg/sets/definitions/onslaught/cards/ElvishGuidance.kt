package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap

/**
 * Elvish Guidance
 * {2}{G}
 * Enchantment — Aura
 * Enchant land
 * Whenever enchanted land is tapped for mana, its controller adds an additional {G}
 * for each Elf on the battlefield.
 */
val ElvishGuidance = card("Elvish Guidance") {
    manaCost = "{2}{G}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nWhenever enchanted land is tapped for mana, its controller adds an additional {G} for each Elf on the battlefield."

    auraTarget = Targets.Land

    staticAbility {
        ability = AdditionalManaOnTap(
            color = Color.GREEN,
            amount = DynamicAmounts.creaturesWithSubtype(Subtype("Elf"))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "255"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        flavorText = "\"Old home never forgotten, new home ours forever.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8698c46b-2628-4482-88f9-e37a01ade274.jpg?1562926194"
    }
}

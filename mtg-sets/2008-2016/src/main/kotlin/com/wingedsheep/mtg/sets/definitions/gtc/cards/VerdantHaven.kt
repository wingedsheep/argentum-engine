package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Verdant Haven
 * {2}{G}
 * Enchantment — Aura
 * Enchant land
 * When this Aura enters, you gain 2 life.
 * Whenever enchanted land is tapped for mana, its controller adds an additional one mana of any color.
 *
 * Fertile Ground's [AdditionalManaOnTap] (`anyColor = true`) tap bonus plus an enters-the-battlefield
 * life gain.
 */
val VerdantHaven = card("Verdant Haven") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nWhen this Aura enters, you gain 2 life.\n" +
        "Whenever enchanted land is tapped for mana, its controller adds an additional one mana of any color."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(2)
        description = "you gain 2 life."
    }

    staticAbility {
        ability = AdditionalManaOnTap(amount = DynamicAmounts.fixed(1), anyColor = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c59109a0-fdab-49cb-bbf6-d405de4d1645.jpg?1783940114"
    }
}

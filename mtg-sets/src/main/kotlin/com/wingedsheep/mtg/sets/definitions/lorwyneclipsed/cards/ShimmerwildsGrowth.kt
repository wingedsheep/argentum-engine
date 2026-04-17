package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantChosenColor
import com.wingedsheep.sdk.scripting.OverrideEnchantedLandManaColor
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Shimmerwilds Growth
 * {1}{G}
 * Enchantment — Aura
 * Enchant land
 * As this Aura enters, choose a color.
 * Enchanted land is the chosen color.
 * Whenever enchanted land is tapped for mana, its controller adds an additional
 * one mana of the chosen color.
 */
val ShimmerwildsGrowth = card("Shimmerwilds Growth") {
    manaCost = "{1}{G}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nAs this Aura enters, choose a color.\nEnchanted land is the chosen color.\nWhenever enchanted land is tapped for mana, its controller adds an additional one mana of the chosen color."

    auraTarget = Targets.Land

    replacementEffect(EntersWithChoice(ChoiceType.COLOR))

    staticAbility {
        ability = GrantChosenColor()
    }

    // "Enchanted land is the chosen color" also swaps its mana output — a Mountain
    // enchanted by this aura (Blue chosen) taps for {U}, not {R}.
    staticAbility {
        ability = OverrideEnchantedLandManaColor(color = null)
    }

    staticAbility {
        ability = AdditionalManaOnTap(
            color = null,
            amount = DynamicAmount.Fixed(1)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Jorge Jacinto"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c122719c-f0d1-4170-a0d1-d62172df1d21.jpg?1767732869"
        ruling("2025-11-17", "If Shimmerwilds Growth is somehow on the battlefield without a chosen color, its last ability won't add any mana.")
        ruling("2025-11-17", "Shimmerwilds Growth's last ability is a mana ability. It doesn't use the stack and can't be responded to.")
    }
}

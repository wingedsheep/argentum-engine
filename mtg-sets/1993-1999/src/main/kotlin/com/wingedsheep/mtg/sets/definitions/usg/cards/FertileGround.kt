package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Fertile Ground
 * {1}{G}
 * Enchantment — Aura
 * Enchant land
 * Whenever enchanted land is tapped for mana, its controller adds an additional one mana of any color.
 *
 * Invasion engine gap #3. Reuses the existing [AdditionalManaOnTap] tap-bonus static, extended with
 * `anyColor = true` so the bonus is one mana of any color the controller chooses each time the land
 * is tapped (rather than Elvish Guidance's fixed {G}). On a manual tap the controller is prompted
 * for the color; when auto-tapping for a cost the solver treats the bonus as flexible.
 */
val FertileGround = card("Fertile Ground") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nWhenever enchanted land is tapped for mana, its controller adds an additional one mana of any color."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = AdditionalManaOnTap(amount = DynamicAmounts.fixed(1), anyColor = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "252"
        artist = "Heather Hudson"
        flavorText = "The forest was too lush for the brothers to despoil—almost."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/091dda35-59e5-456d-8804-61513a610aed.jpg?1783946315"
    }
}

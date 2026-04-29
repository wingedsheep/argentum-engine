package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Boneclub Berserker
 * {3}{R}
 * Creature — Goblin Berserker
 * 2/4
 * This creature gets +2/+0 for each other Goblin you control.
 */
val BoneclubBerserker = card("Boneclub Berserker") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Goblin Berserker"
    power = 2
    toughness = 4
    oracleText = "This creature gets +2/+0 for each other Goblin you control."

    staticAbility {
        ability = GrantDynamicStatsEffect(
            target = StaticTarget.SourceCreature,
            powerBonus = DynamicAmount.Multiply(
                amount = DynamicAmounts.otherCreaturesWithSubtypeYouControl(Subtype.GOBLIN),
                multiplier = 2
            ),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Slawomir Maniak"
        flavorText = "Bolstered by the cheers of his warren, Trugg was ready to make his appearance in Auntie Grub's tales."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3dbbe30-3d6e-46f8-92c1-caee995cba1a.jpg?1767732716"
    }
}

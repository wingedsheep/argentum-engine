package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.DynamicAmount
import com.wingedsheep.rulesengine.ability.GrantDynamicStatsEffect
import com.wingedsheep.rulesengine.ability.StaticTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Boneclub Berserker
 *
 * {3}{R} Creature — Goblin Berserker 2/4
 * This creature gets +2/+0 for each other Goblin you control.
 */
object BoneclubBerserker {
    val definition = CardDefinition.creature(
        name = "Boneclub Berserker",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype.GOBLIN, Subtype.BERSERKER),
        power = 2,
        toughness = 4,
        oracleText = "This creature gets +2/+0 for each other Goblin you control.",
        metadata = ScryfallMetadata(
            collectorNumber = "126",
            rarity = Rarity.COMMON,
            artist = "Slawomir Maniak",
            imageUri = "https://cards.scryfall.io/normal/front/e/e/ee2e2e2e-2e2e-2e2e-2e2e-2e2e2e2e2e2e.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Boneclub Berserker") {
        // +2/+0 for each other Goblin you control
        staticAbility(
            GrantDynamicStatsEffect(
                target = StaticTarget.SourceCreature,
                powerBonus = DynamicAmount.OtherCreaturesWithSubtypeYouControl(Subtype.GOBLIN),
                toughnessBonus = DynamicAmount.Fixed(0)
            )
        )
    }
}

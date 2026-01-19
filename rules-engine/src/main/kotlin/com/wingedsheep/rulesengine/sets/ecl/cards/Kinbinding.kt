package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.CreateTokenEffect
import com.wingedsheep.rulesengine.ability.DynamicAmount
import com.wingedsheep.rulesengine.ability.GrantDynamicStatsEffect
import com.wingedsheep.rulesengine.ability.OnBeginCombat
import com.wingedsheep.rulesengine.ability.StaticTarget
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost

/**
 * Kinbinding
 *
 * {3}{W}{W} Enchantment
 * Creatures you control get +X/+X, where X is the number of creatures
 * that entered the battlefield under your control this turn.
 * At the beginning of combat on your turn, create a 1/1 green and white
 * Kithkin creature token.
 */
object Kinbinding {
    val definition = CardDefinition.enchantment(
        name = "Kinbinding",
        manaCost = ManaCost.parse("{3}{W}{W}"),
        oracleText = "Creatures you control get +X/+X, where X is the number of creatures " +
                "that entered the battlefield under your control this turn.\n" +
                "At the beginning of combat on your turn, create a 1/1 green and white " +
                "Kithkin creature token.",
        metadata = ScryfallMetadata(
            collectorNumber = "20",
            rarity = Rarity.RARE,
            artist = "Caio Monteiro",
            flavorText = "Kithkin are bound through the thoughtweft—mind to mind and heart to heart.",
            imageUri = "https://cards.scryfall.io/normal/front/b/6/b6e35fd5-de7e-40a8-a23c-00d07fd1ac56.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Kinbinding") {
        // Static ability: Creatures you control get +X/+X where X = creatures ETB this turn
        staticAbility(
            GrantDynamicStatsEffect(
                target = StaticTarget.AllControlledCreatures,
                powerBonus = DynamicAmount.CreaturesEnteredThisTurn,
                toughnessBonus = DynamicAmount.CreaturesEnteredThisTurn
            )
        )

        // At the beginning of combat on your turn, create a 1/1 G/W Kithkin token
        triggered(
            trigger = OnBeginCombat(controllerOnly = true),
            effect = CreateTokenEffect(
                count = 1,
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN, Color.WHITE),
                creatureTypes = setOf("Kithkin")
            )
        )
    }
}

package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kinbinding
 * {3}{W}{W}
 * Enchantment
 *
 * Creatures you control get +X/+X, where X is the number of creatures that entered
 * the battlefield under your control this turn.
 * At the beginning of combat on your turn, create a 1/1 green and white Kithkin creature token.
 */
val Kinbinding = card("Kinbinding") {
    manaCost = "{3}{W}{W}"
    typeLine = "Enchantment"
    oracleText = "Creatures you control get +X/+X, where X is the number of creatures that entered the battlefield under your control this turn.\n" +
        "At the beginning of combat on your turn, create a 1/1 green and white Kithkin creature token."

    staticAbility {
        ability = GrantDynamicStatsEffect(
            target = StaticTarget.AllControlledCreatures,
            powerBonus = DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Creature.enteredThisTurn()
            ),
            toughnessBonus = DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Creature.enteredThisTurn()
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Kithkin"),
            imageUri = "https://cards.scryfall.io/normal/front/2/e/2ed11e1b-2289-48d2-8d96-ee7e590ecfd4.jpg?1767955680"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Caio Monteiro"
        flavorText = "Kithkin are bound through the thoughtweft\u2014mind to mind and heart to heart."
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6e35fd5-de7e-40a8-a23c-00d07fd1ac56.jpg?1767658490"
    }
}

package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Ancient Copper Dragon
 * {4}{R}{R}
 * Creature — Elder Dragon
 * 6/5
 * Flying
 * Whenever this creature deals combat damage to a player, roll a d20. You create a number of
 * Treasure tokens equal to the result.
 *
 * The roll's result (CR 706.2 — after any modifiers) is captured with `rollDiceResult` and read
 * back as the dynamic Treasure count, all within one resolution.
 */
val AncientCopperDragon = card("Ancient Copper Dragon") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elder Dragon"
    power = 6
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, roll a d20. You create a number of " +
        "Treasure tokens equal to the result."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            run(Effects.CreateTreasure(result.amount))
        }
        description = "Whenever this creature deals combat damage to a player, roll a d20. You create " +
            "a number of Treasure tokens equal to the result."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "161"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/3836dddd-a7e4-499f-ad49-ce298aa65720.jpg?1783922745"
    }
}

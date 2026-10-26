package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Ancient Gold Dragon
 * {5}{W}{W}
 * Creature — Elder Dragon
 * 7/10
 * Flying
 * Whenever this creature deals combat damage to a player, roll a d20. You create a number of 1/1
 * blue Faerie Dragon creature tokens with flying equal to the result.
 *
 * The roll's result is captured with `rollDiceResult` and read back as the dynamic token count.
 */
val AncientGoldDragon = card("Ancient Gold Dragon") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elder Dragon"
    power = 7
    toughness = 10
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, roll a d20. You create a number of 1/1 " +
        "blue Faerie Dragon creature tokens with flying equal to the result."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            run(
                Effects.CreateToken(
                    count = result.amount,
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Faerie", "Dragon"),
                    keywords = setOf(Keyword.FLYING),
                    imageUri = "https://cards.scryfall.io/normal/front/6/f/6f1eb498-8496-4234-8e87-78dc3f647534.jpg?1783922323",
                )
            )
        }
        description = "Whenever this creature deals combat damage to a player, roll a d20. You create " +
            "a number of 1/1 blue Faerie Dragon creature tokens with flying equal to the result."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "3"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/421395b1-2694-42fd-bb90-0007e78adefc.jpg?1783922822"
    }
}

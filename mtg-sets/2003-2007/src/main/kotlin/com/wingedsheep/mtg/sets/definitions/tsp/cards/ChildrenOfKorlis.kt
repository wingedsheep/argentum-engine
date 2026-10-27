package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Children of Korlis — Time Spiral #8
 * {W} · Creature — Human Rebel Cleric · 1/1
 *
 * Sacrifice this creature: You gain life equal to the life you've lost this turn.
 *
 * `lifeLostThisTurn` is the total life lost (damage, life-loss effects, life paid), never netted
 * against life gained — exactly the ruling's reading.
 */
val ChildrenOfKorlis = card("Children of Korlis") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Rebel Cleric"
    power = 1
    toughness = 1
    oracleText = "Sacrifice this creature: You gain life equal to the life you've lost this turn. " +
        "(Damage causes loss of life.)"

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.GainLife(DynamicAmounts.lifeLostThisTurn(Player.You))
        description = "Sacrifice this creature: You gain life equal to the life you've lost this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Quinton Hoover"
        flavorText = "\"We have a proud history of self-sacrifice. But it is easy, in these bleak times, to " +
            "find one among us who is eager to die for any cause.\"\n—Tavalus, acolyte of Korlis"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/221a5895-bc21-4f10-b5fc-a4980fde843e.jpg?1783943257"
        ruling(
            "2021-03-19",
            "The life you gain is based on the total amount of life you lost, not the difference in your life " +
                "total from when the turn started. For example, if you lose 5 life and gain 3 life before " +
                "activating the ability, the ability will cause you to gain 5 life, not 2."
        )
    }
}

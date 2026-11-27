package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fettergeist
 * {2}{U}
 * Creature — Spirit
 * 3/4
 *
 * Flying
 * At the beginning of your upkeep, sacrifice this creature unless you pay {1} for each other
 * creature you control.
 *
 * The amount is counted when the trigger resolves (2012-05-01 ruling), and `excludeSelf` leaves
 * Fettergeist itself out of the count rather than subtracting a fixed 1.
 */
val Fettergeist = card("Fettergeist") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "At the beginning of your upkeep, sacrifice this creature unless you pay {1} for each other " +
        "creature you control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.MayPay(
            cost = Effects.PayDynamicMana(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature, excludeSelf = true).count()
            ),
            then = Effects.Nothing,
            otherwise = Effects.SacrificeTarget(EffectTarget.Self),
        )
        description = "At the beginning of your upkeep, sacrifice this creature unless you pay {1} for " +
            "each other creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Izzy"
        flavorText = "The choices of the living will tether them in death."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e89ef0e-1bfe-4e12-90ee-38f993cd8110.jpg?1783940722"
        ruling("2012-05-01", "The amount of mana you need to pay is determined when the triggered ability resolves.")
    }
}

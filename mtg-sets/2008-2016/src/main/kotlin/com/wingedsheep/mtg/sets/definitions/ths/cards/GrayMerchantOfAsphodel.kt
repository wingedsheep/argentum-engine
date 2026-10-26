package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gray Merchant of Asphodel
 * {3}{B}{B}
 * Creature — Zombie
 * 2/4
 * When this creature enters, each opponent loses X life, where X is your devotion to black. You
 * gain life equal to the life lost this way.
 *
 * `DrainLife` is exactly this shape: each opponent loses the amount, then you gain the *total* life
 * actually lost (the ruling: not simply X). Devotion is evaluated as the trigger resolves, counting
 * the Merchant itself if it's still on the battlefield.
 */
val GrayMerchantOfAsphodel = card("Gray Merchant of Asphodel") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, each opponent loses X life, where X is your devotion to " +
        "black. You gain life equal to the life lost this way. (Each {B} in the mana costs of " +
        "permanents you control counts toward your devotion to black.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrainLife(DynamicAmounts.devotionTo(Color.BLACK))
        description = "When this creature enters, each opponent loses X life, where X is your devotion " +
            "to black. You gain life equal to the life lost this way."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Robbie Trevino"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b06078ce-f534-4e16-9a70-d51620a33eb2.jpg?1783939779"
        ruling("2021-03-19", "The amount of life you gain is the total amount of life lost, not simply the value of X.")
        ruling(
            "2020-01-24",
            "Hybrid mana symbols, monocolored hybrid mana symbols, and Phyrexian mana symbols do count " +
                "toward your devotion to their color(s)."
        )
        ruling(
            "2020-01-24",
            "If an activated ability or triggered ability has an effect that depends on your devotion to a " +
                "color, you count the number of mana symbols of that color among the mana costs of " +
                "permanents you control as the ability resolves. The permanent with that ability will be " +
                "counted if it's still on the battlefield at that time."
        )
    }
}

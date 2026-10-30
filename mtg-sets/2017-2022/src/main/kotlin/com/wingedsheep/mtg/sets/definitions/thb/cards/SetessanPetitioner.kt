package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Setessan Petitioner
 * {1}{G}{G}
 * Creature — Human Druid
 * 2/2
 * When this creature enters, you gain life equal to your devotion to green.
 *
 * Devotion is counted as the trigger resolves (ruling), so the Petitioner's own {G}{G} counts if
 * it's still on the battlefield then.
 */
val SetessanPetitioner = card("Setessan Petitioner") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you gain life equal to your devotion to green. " +
        "(Each {G} in the mana costs of permanents you control counts toward your devotion to green.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(DynamicAmounts.devotionTo(Color.GREEN))
        description = "When this creature enters, you gain life equal to your devotion to green."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Livia Prima"
        flavorText = "\"Cypress leaves for family, larkspur for protection, amaranth for a bountiful harvest.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/8/384495c2-238b-4f8d-89c2-e05b64f9dde0.jpg?1783931529"
        ruling(
            "2020-01-24",
            "If an activated ability or triggered ability has an effect that depends on your devotion to a " +
                "color, you count the number of mana symbols of that color among the mana costs of permanents " +
                "you control as the ability resolves. The permanent with that ability will be counted if it's " +
                "still on the battlefield at that time."
        )
    }
}

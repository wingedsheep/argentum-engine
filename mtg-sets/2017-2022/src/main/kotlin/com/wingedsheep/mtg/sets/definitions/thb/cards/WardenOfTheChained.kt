package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Warden of the Chained
 * {1}{R}{G}
 * Creature — Minotaur Warrior
 * 4/4
 * Trample
 * This creature can't attack unless you control another creature with power 4 or greater.
 *
 * Rhonas the Indomitable's attack restriction: [CantAttackUnless] over [Conditions.YouControl] with
 * `excludeSelf`, so the Warden's own power never satisfies it — while a second Warden does (ruling).
 */
val WardenOfTheChained = card("Warden of the Chained") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Minotaur Warrior"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "This creature can't attack unless you control another creature with power 4 or greater."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = CantAttackUnless(
            Conditions.YouControl(GameObjectFilter.Creature.powerAtLeast(4), excludeSelf = true)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Steve Prescott"
        flavorText = "\"Strength, skill, rage—all have their limits, but patience can be infinite.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff48d55e-aa8b-4061-9c77-fd7e696fd7ba.jpg?1783931518"
        ruling(
            "2020-01-24",
            "Once Warden of the Chained is attacking, it will remain attacking even if you no longer control " +
                "another creature with power 4 or greater."
        )
        ruling(
            "2020-01-24",
            "If you control two Wardens of the Chained, each will fulfill the other's condition and both will " +
                "be able to attack."
        )
    }
}

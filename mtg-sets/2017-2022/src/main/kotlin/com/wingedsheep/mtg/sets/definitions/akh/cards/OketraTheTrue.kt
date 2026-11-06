package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Oketra the True
 * {3}{W}
 * Legendary Creature — God
 * 3/6
 * Double strike, indestructible
 * Oketra can't attack or block unless you control at least three other creatures.
 * {3}{W}: Create a 1/1 white Warrior creature token with vigilance.
 *
 * The combat restriction is the Kefnet/Rhonas/Hazoret/Bontu pair of statics over one condition;
 * `YouControlOtherAtLeast` leaves Oketra herself out of the creature count.
 */
val OketraTheTrue = card("Oketra the True") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — God"
    power = 3
    toughness = 6
    oracleText = "Double strike, indestructible\n" +
        "Oketra can't attack or block unless you control at least three other creatures.\n" +
        "{3}{W}: Create a 1/1 white Warrior creature token with vigilance."

    keywords(Keyword.DOUBLE_STRIKE, Keyword.INDESTRUCTIBLE)

    val threeOtherCreatures = Conditions.YouControlOtherAtLeast(3, GameObjectFilter.Creature)

    staticAbility {
        ability = CantAttackUnless(threeOtherCreatures)
    }
    staticAbility {
        ability = CantBlockUnless(threeOtherCreatures)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Warrior"),
            keywords = setOf(Keyword.VIGILANCE),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "21"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2cc79cf-d4db-4236-b3e2-fd12662b2a74.jpg?1783936537"
        ruling("2017-04-18", "Once Oketra has attacked or blocked, it will remain in combat even if the number of other creatures you control becomes two or fewer.")
        ruling("2017-04-18", "You don't have to attack with three other creatures for Oketra to be able to attack. The same is true of blocking.")
    }
}

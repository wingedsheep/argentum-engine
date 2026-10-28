package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Open the Graves
 * {3}{B}{B}
 * Enchantment
 * Whenever a nontoken creature you control dies, create a 2/2 black Zombie creature token.
 */
val OpenTheGraves = card("Open the Graves") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a nontoken creature you control dies, create a 2/2 black Zombie creature token."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().nontoken()).dies()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Vincent Proce"
        flavorText = "It was a night like any other. It would soon become a night no one would ever forget."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9ced1022-abd9-4e22-a4f2-fef738295224.jpg?1783934563"
        ruling(
            "2018-07-13",
            "If another creature you control dies at the same time that Open the Graves leaves the battlefield, you'll get a Zombie token."
        )
    }
}

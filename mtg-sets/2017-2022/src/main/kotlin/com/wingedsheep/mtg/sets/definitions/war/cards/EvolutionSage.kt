package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Evolution Sage
 * {2}{G}
 * Creature — Elf Druid
 * 3/2
 * Landfall — Whenever a land you control enters, proliferate.
 */
val EvolutionSage = card("Evolution Sage") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 3
    toughness = 2
    oracleText = "Landfall — Whenever a land you control enters, proliferate. (Choose any number of " +
        "permanents and/or players, then give each another counter of each kind already there.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Simon Dominic"
        flavorText = "\"To care for yourself, cultivate the world. To care for the world, cultivate yourself.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4042db8-6032-49a2-bc96-ad15f55db6c3.jpg?1783933415"
    }
}

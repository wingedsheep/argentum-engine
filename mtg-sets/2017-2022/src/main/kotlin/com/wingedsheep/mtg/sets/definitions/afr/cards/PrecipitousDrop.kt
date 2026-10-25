package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Precipitous Drop
 * {2}{B}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, venture into the dungeon. (Enter the first room or advance to the next room.)
 * Enchanted creature gets -2/-2. It gets -5/-5 instead as long as you've completed a dungeon.
 *
 * "Instead" is modelled as two mutually exclusive conditional statics, so exactly one of
 * -2/-2 or -5/-5 applies at any time. "You" is the Aura's controller.
 */
val PrecipitousDrop = card("Precipitous Drop") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, venture into the dungeon. (Enter the first room or advance to the next room.)\n" +
        "Enchanted creature gets -2/-2. It gets -5/-5 instead as long as you've completed a dungeon."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.VentureIntoTheDungeon()
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(-2, -2),
            condition = Conditions.Not(Conditions.YouCompletedADungeon)
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(-5, -5),
            condition = Conditions.YouCompletedADungeon
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Gabor Szikszai"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/7293f9b4-565a-4065-940b-543f76ebecae.jpg?1783926491"
        ruling(
            "2021-07-23",
            "Precipitous Drop does not need to be on the battlefield when you complete the dungeon in order for the enchanted creature to get -5/-5."
        )
        ruling(
            "2021-07-23",
            "The controller of Precipitous Drop ventures into the dungeon, not the controller of the enchanted creature. Similarly, Precipitous Drop cares whether its controller has completed a dungeon, not whether the creature's controller has completed a dungeon."
        )
    }
}

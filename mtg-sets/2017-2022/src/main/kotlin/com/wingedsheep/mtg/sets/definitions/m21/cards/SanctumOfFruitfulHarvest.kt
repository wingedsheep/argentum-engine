package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sanctum of Fruitful Harvest — Core Set 2021 #203
 * {2}{G} · Legendary Enchantment — Shrine
 *
 * At the beginning of your first main phase, add X mana of any one color, where X is the number
 * of Shrines you control.
 *
 * Black Market's first-main-phase mana shape with Gilded Lotus's "any one color" choice
 * ([Effects.AddAnyColorMana]): one color is chosen and all X mana is of it.
 */
val SanctumOfFruitfulHarvest = card("Sanctum of Fruitful Harvest") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Enchantment — Shrine"
    oracleText = "At the beginning of your first main phase, add X mana of any one color, where X is the " +
        "number of Shrines you control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.AddAnyColorMana(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count()
        )
        description = "At the beginning of your first main phase, add X mana of any one color, where X is the " +
            "number of Shrines you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Johannes Voss"
        flavorText = "Beneath bright blossoms\nVerdant with spring's rich bounty,\nThe soul blooms in song."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5ca28cfd-1a1b-4176-8b07-993d3d7b7ae4.jpg?1783930670"
        ruling(
            "2020-06-23",
            "Each Shrine has an ability that counts the number of Shrines you control. These abilities include the Shrine they're printed on."
        )
        ruling(
            "2020-06-23",
            "Shrines count only enchantments with the subtype Shrine. Other cards with \"shrine\" in their name (such as Jungle Shrine, Luxa River Shrine, and Nantuko Shrine) don't count."
        )
    }
}

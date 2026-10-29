package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sanctum of Shattered Heights — Core Set 2021 #157
 * {2}{R} · Legendary Enchantment — Shrine
 *
 * {1}, Discard a land card or Shrine card: Sanctum of Shattered Heights deals X damage to target
 * creature or planeswalker, where X is the number of Shrines you control.
 *
 * Molten Vortex's discard-a-land cost widened to "land or Shrine card". X is counted as the
 * ability resolves; a Shrine discarded to pay the cost was never on the battlefield, so it doesn't
 * count.
 */
val SanctumOfShatteredHeights = card("Sanctum of Shattered Heights") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Enchantment — Shrine"
    oracleText = "{1}, Discard a land card or Shrine card: Sanctum of Shattered Heights deals X damage to " +
        "target creature or planeswalker, where X is the number of Shrines you control."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Discard(GameObjectFilter.Land or GameObjectFilter.Any.withSubtype("Shrine")),
        )
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count(),
            victim,
        )
        description = "{1}, Discard a land card or Shrine card: Sanctum of Shattered Heights deals X damage to " +
            "target creature or planeswalker, where X is the number of Shrines you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Johannes Voss"
        flavorText = "Rage smolders within,\nAn all-consuming wildfire\nBurning fear to ash"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/482ae7f4-3950-4766-8392-eea753e29426.jpg?1783930686"
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

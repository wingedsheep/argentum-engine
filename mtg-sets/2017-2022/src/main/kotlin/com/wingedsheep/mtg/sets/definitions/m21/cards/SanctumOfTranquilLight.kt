package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sanctum of Tranquil Light — Core Set 2021 #33
 * {W} · Legendary Enchantment — Shrine
 *
 * {5}{W}: Tap target creature. This ability costs {1} less to activate for each Shrine you control.
 *
 * The discount rides `genericCostReduction` (Battlefield Butcher's shape): only the generic {5}
 * shrinks, never the {W} (ruling), and the Shrine count includes this one.
 */
val SanctumOfTranquilLight = card("Sanctum of Tranquil Light") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Legendary Enchantment — Shrine"
    oracleText = "{5}{W}: Tap target creature. This ability costs {1} less to activate for each Shrine you control."

    activatedAbility {
        cost = Costs.Mana("{5}{W}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
        genericCostReduction =
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count()
        description = "{5}{W}: Tap target creature. This ability costs {1} less to activate for each Shrine you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Johannes Voss"
        flavorText = "An untroubled mind\nDwells in perfect harmony\nWith a peaceful heart."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34a5478a-1a2c-4117-b543-da083ed2b562.jpg?1783930735"
        ruling(
            "2020-06-23",
            "The cost reduction applies only to generic mana in the cost of the activated ability. It can't reduce the {W} requirement."
        )
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

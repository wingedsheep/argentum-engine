package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sanctum of Stone Fangs — Core Set 2021 #120
 * {1}{B} · Legendary Enchantment — Shrine
 *
 * At the beginning of your first main phase, each opponent loses X life and you gain X life,
 * where X is the number of Shrines you control.
 *
 * Two independent amounts, not a "life lost this way" drain: you gain exactly X however many
 * opponents lose it (the Two-Headed Giant ruling — the team loses twice X, you gain X).
 */
val SanctumOfStoneFangs = card("Sanctum of Stone Fangs") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Enchantment — Shrine"
    oracleText = "At the beginning of your first main phase, each opponent loses X life and you gain X " +
        "life, where X is the number of Shrines you control."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        val shrines = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count()
        effect = Effects.LoseLife(shrines, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(shrines)
        description = "At the beginning of your first main phase, each opponent loses X life and you gain X " +
            "life, where X is the number of Shrines you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Johannes Voss"
        flavorText = "Renounce all limits,\nFalse chains binding potential.\nPower knows no bounds."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1439412-618c-483d-89b9-5ea37f0f1edc.jpg?1783930700"
        ruling(
            "2020-06-23",
            "If Sanctum of Stone Fangs leaves the battlefield while its ability is on the stack, you may control no Shrines as the ability resolves. In that case, no player gains or loses any life."
        )
        ruling(
            "2020-06-23",
            "In a Two-Headed Giant game, Sanctum of Stone Fangs causes the opposing team to lose twice X life and you to gain X life."
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

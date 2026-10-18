package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Go-Shintai of Shared Purpose — Kamigawa: Neon Dynasty #14 (canonical printing)
 * {3}{W} · Legendary Enchantment Creature — Shrine · 1/3
 *
 * Vigilance
 * At the beginning of your end step, you may pay {1}. If you do, create a 1/1 colorless Spirit
 * creature token for each Shrine you control.
 *
 * "You may pay {1}. If you do, …" is an optional cost rider on the trigger itself (`Effects.MayPay`),
 * not a reflexive trigger. The Shrine count is read on resolution and includes the Go-Shintai itself.
 */
val GoShintaiOfSharedPurpose = card("Go-Shintai of Shared Purpose") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Enchantment Creature — Shrine"
    power = 1
    toughness = 3
    oracleText = "Vigilance\nAt the beginning of your end step, you may pay {1}. If you do, create a 1/1 " +
        "colorless Spirit creature token for each Shrine you control."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.MayPay(
            ManaCost.parse("{1}"),
            Effects.CreateToken(
                count = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Shrine")).count(),
                power = 1,
                toughness = 1,
                creatureTypes = setOf("Spirit"),
            ),
        )
        description = "At the beginning of your end step, you may pay {1}. If you do, create a 1/1 " +
            "colorless Spirit creature token for each Shrine you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Johannes Voss"
        flavorText = "It seeks those who act with kindness and offers friendship."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0ab91962-ebad-46f6-9f90-7477c224d93d.jpg?1783923924"
    }
}

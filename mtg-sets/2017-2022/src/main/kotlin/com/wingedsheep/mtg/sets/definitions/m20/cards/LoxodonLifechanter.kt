package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Loxodon Lifechanter — Core Set 2020 #27
 * {5}{W} · Creature — Elephant Cleric · 4/6
 *
 * When this creature enters, you may have your life total become the total toughness of
 * creatures you control.
 * {5}{W}: This creature gets +X/+X until end of turn, where X is your life total.
 */
val LoxodonLifechanter = card("Loxodon Lifechanter") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elephant Cleric"
    power = 4
    toughness = 6
    oracleText = "When this creature enters, you may have your life total become the total toughness " +
        "of creatures you control.\n" +
        "{5}{W}: This creature gets +X/+X until end of turn, where X is your life total."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.SetLifeTotal(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumToughness(),
                EffectTarget.Controller
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{5}{W}")
        effect = Effects.ModifyStats(
            DynamicAmounts.yourLifeTotal(),
            DynamicAmounts.yourLifeTotal(),
            EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "27"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19610e6d-61bf-4c79-bfb2-e855eb944d11.jpg?1783933025"
    }
}

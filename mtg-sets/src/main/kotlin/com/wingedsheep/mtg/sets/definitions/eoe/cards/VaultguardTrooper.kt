package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.IfYouDoEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Vaultguard Trooper
 * {4}{R}
 * Creature — Kavu Soldier
 * 5/5
 *
 * At the beginning of your end step, if you control two or more tapped creatures,
 * you may discard your hand. If you do, draw two cards.
 */
val VaultguardTrooper = card("Vaultguard Trooper") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Kavu Soldier"
    power = 5
    toughness = 5
    oracleText = "At the beginning of your end step, if you control two or more tapped creatures, " +
        "you may discard your hand. If you do, draw two cards."

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature.tapped()),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(2)
        )
        effect = MayEffect(
            IfYouDoEffect(
                action = EffectPatterns.discardHand(EffectTarget.Controller),
                ifYouDo = Effects.DrawCards(2)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "David Palumbo"
        flavorText = "\"Give everything and advance!\"\n—Kav naval motto"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36afe3b1-43a2-47b4-bc0a-24efb1e2e5a0.jpg?1752947227"

        ruling(
            "2025-07-25",
            "Vaultguard Trooper's ability will check as your end step starts to see if you control " +
                "two or more tapped creatures. If you don't, the ability won't trigger at all. You won't " +
                "be able to tap anything during your end step in time to have the ability trigger. If you " +
                "don't control two or more tapped creatures when the ability resolves, the ability won't " +
                "do anything."
        )
    }
}

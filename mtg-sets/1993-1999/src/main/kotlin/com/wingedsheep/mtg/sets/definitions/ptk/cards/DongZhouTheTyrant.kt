package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dong Zhou, the Tyrant
 * {4}{R}
 * Legendary Creature — Human Soldier
 * 3/3
 * When Dong Zhou enters, target creature an opponent controls deals damage equal to its power to that player.
 */
val DongZhouTheTyrant = card("Dong Zhou, the Tyrant") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "When Dong Zhou enters, target creature an opponent controls deals damage equal to its power to that player."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.opponentControls())
        effect = Effects.DealDamage(DynamicAmounts.powerOf(t), EffectTarget.TargetController, damageSource = t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Inoue Junichi"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51ccdca0-2c29-4b5c-ba5a-364cc6945801.jpg?1783946107"
    }
}

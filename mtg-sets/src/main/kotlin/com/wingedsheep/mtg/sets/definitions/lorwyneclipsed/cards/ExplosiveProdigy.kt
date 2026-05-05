package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Explosive Prodigy
 * {1}{R}
 * Creature — Elemental Sorcerer
 * 1/1
 *
 * Vivid — When this creature enters, it deals X damage to target creature an opponent
 * controls, where X is the number of colors among permanents you control.
 */
val ExplosiveProdigy = card("Explosive Prodigy") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Elemental Sorcerer"
    oracleText = "Vivid — When this creature enters, it deals X damage to target creature an " +
        "opponent controls, where X is the number of colors among permanents you control."
    power = 1
    toughness = 1

    keywords(Keyword.VIVID)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val victim = target("creature", Targets.CreatureOpponentControls)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.colorsAmongPermanents(),
            target = victim,
            damageSource = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Joshua Raphael"
        flavorText = "\"Intense flame, but malleable. The braiders can help you harness that heat.\"\n" +
            "—Senbrand, soulstoke mentor"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5515ea5e-ce28-4938-a31e-5e48522a5f93.jpg?1767658177"
        ruling("2025-11-17", "The value of X is calculated only once, as Explosive Prodigy's ability resolves.")
    }
}

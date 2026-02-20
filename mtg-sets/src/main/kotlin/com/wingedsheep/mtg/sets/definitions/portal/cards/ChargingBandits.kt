package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect

/**
 * Charging Bandits
 * {4}{B}
 * Creature — Human Rogue
 * 3/3
 * Whenever Charging Bandits attacks, it gets +2/+0 until end of turn.
 */
val ChargingBandits = card("Charging Bandits") {
    manaCost = "{4}{B}"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = ModifyStatsEffect(
            powerModifier = 2,
            toughnessModifier = 0,
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Michael Sutfin"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/1721ee11-c7ee-4878-b2ab-4f090e0c5def.jpg"
    }
}

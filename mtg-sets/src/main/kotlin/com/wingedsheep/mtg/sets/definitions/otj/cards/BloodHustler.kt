package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Blood Hustler
 * {1}{B}
 * Creature — Vampire Rogue
 * 1/1
 *
 * Whenever you commit a crime, put a +1/+1 counter on this creature. This ability triggers only once
 * each turn. (Targeting opponents, anything they control, and/or cards in their graveyards is a crime.)
 * {3}{B}: Target opponent loses 1 life and you gain 1 life.
 */
val BloodHustler = card("Blood Hustler") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Rogue"
    power = 1
    toughness = 1
    oracleText = "Whenever you commit a crime, put a +1/+1 counter on this creature. " +
        "This ability triggers only once each turn. " +
        "(Targeting opponents, anything they control, and/or cards in their graveyards is a crime.)\n" +
        "{3}{B}: Target opponent loses 1 life and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        oncePerTurn = true
        effect = AddCountersEffect(
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            count = 1,
            target = EffectTarget.Self
        )
        description = "Whenever you commit a crime, put a +1/+1 counter on this creature. " +
            "This ability triggers only once each turn."
    }

    activatedAbility {
        cost = Costs.Mana("{3}{B}")
        val t = target("target", TargetOpponent())
        effect = Effects.Composite(
            LoseLifeEffect(1, t),
            GainLifeEffect(1, EffectTarget.Controller)
        )
        description = "{3}{B}: Target opponent loses 1 life and you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Anna Pavleeva"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1016a750-2a18-4443-a600-957eb4026d3a.jpg?1712355556"
    }
}

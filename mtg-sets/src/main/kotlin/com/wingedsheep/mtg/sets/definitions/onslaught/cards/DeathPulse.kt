package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.OnCycle

/**
 * Death Pulse
 * {2}{B}{B}
 * Instant
 * Target creature gets -4/-4 until end of turn.
 * Cycling {1}{B}{B}
 * When you cycle Death Pulse, you may have target creature get -1/-1 until end of turn.
 */
val DeathPulse = card("Death Pulse") {
    manaCost = "{2}{B}{B}"
    typeLine = "Instant"
    oracleText = "Target creature gets -4/-4 until end of turn.\nCycling {1}{B}{B}\nWhen you cycle Death Pulse, you may have target creature get -1/-1 until end of turn."

    spell {
        target = Targets.Creature
        effect = ModifyStatsEffect(
            powerModifier = -4,
            toughnessModifier = -4,
            target = EffectTarget.ContextTarget(0),
            duration = Duration.EndOfTurn
        )
    }

    keywordAbility(KeywordAbility.cycling("{1}{B}{B}"))

    triggeredAbility {
        trigger = OnCycle(controllerOnly = true)
        target = Targets.Creature
        effect = MayEffect(
            ModifyStatsEffect(
                powerModifier = -1,
                toughnessModifier = -1,
                target = EffectTarget.ContextTarget(0),
                duration = Duration.EndOfTurn
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Tony Szczudlo"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/5/2/524fd470-e535-47ea-98a0-6187e429dfe1.jpg?1562914293"
    }
}

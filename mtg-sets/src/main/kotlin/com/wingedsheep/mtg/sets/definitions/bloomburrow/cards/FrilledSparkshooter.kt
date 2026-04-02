package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Frilled Sparkshooter
 * {3}{R}
 * Creature — Lizard Archer
 * 3/3
 * Menace, reach
 * This creature enters with a +1/+1 counter on it if an opponent lost life this turn.
 */
val FrilledSparkshooter = card("Frilled Sparkshooter") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Lizard Archer"
    power = 3
    toughness = 3
    oracleText = "Menace, reach\nThis creature enters with a +1/+1 counter on it if an opponent lost life this turn."

    keywords(Keyword.MENACE, Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.OpponentLostLifeThisTurn
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Danny Schwartz"
        flavorText = "Its frills keep it steady, making its aim impeccable."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/674bbd6d-e329-42cf-963d-88d1ce8fe51e.jpg?1721426623"
    }
}

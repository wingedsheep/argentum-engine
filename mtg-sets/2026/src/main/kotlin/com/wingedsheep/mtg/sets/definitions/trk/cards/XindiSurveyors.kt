package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Xindi Surveyors
 * {2}{G}
 * Creature — Xindi Scientist
 * 3/2
 *
 * Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature.
 */
val XindiSurveyors = card("Xindi Surveyors") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Xindi Scientist"
    colorIdentity = "G"
    oracleText = "Landfall — Whenever a land you control enters, put a +1/+1 counter on this creature."
    power = 3
    toughness = 2
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "225"
        artist = "Joan Piqué Llorens"
        flavorText = "Six sapient species evolved in parallel on Xindus. They learned to turn their differences into strengths . . . but only after civil war reduced their number to five."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/803608aa-ae69-45d2-ae48-f1688d2a6a0d.jpg?1786005515"
    }
}


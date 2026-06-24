package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Michelangelo, Game Master
 * {2}{G}
 * Legendary Creature — Mutant Ninja Turtle
 * 3/3
 *
 * Disappear — At the beginning of your end step, if a permanent left the
 * battlefield under your control this turn, put a +1/+1 counter on Michelangelo.
 */
val MichelangeloGameMaster = card("Michelangelo, Game Master") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Mutant Ninja Turtle"
    oracleText = "Disappear — At the beginning of your end step, if a permanent left the battlefield under your control this turn, put a +1/+1 counter on Michelangelo."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Disappear — At the beginning of your end step, if a permanent left the battlefield under your control this turn, put a +1/+1 counter on Michelangelo."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Eilene Cherie"
        flavorText = "\"A teacher must be ready to accept responsibility for the student, especially when the student is a weirdo.\"\n—Splinter"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e914c3d-2eed-48bf-af9a-a8998fd5111d.jpg?1771502708"
    }
}

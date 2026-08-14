package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pests of Honor
 * {2}{W}
 * Creature — Mouse
 * 2/2
 *
 * Celebration — At the beginning of combat on your turn, if two or more nonland permanents
 * entered the battlefield under your control this turn, put a +1/+1 counter on this creature.
 *
 * The triggered half of the Celebration ability word (CR 207.2c — italic flavor, no rules
 * meaning): an intervening-'if' clause (CR 603.4), so [Conditions.Celebration] is checked both
 * when the begin-combat step starts and again as the ability resolves.
 */
val PestsOfHonor = card("Pests of Honor") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Mouse"
    power = 2
    toughness = 2
    oracleText = "Celebration — At the beginning of combat on your turn, if two or more nonland " +
        "permanents entered the battlefield under your control this turn, put a +1/+1 counter " +
        "on this creature."

    triggeredAbility {
        trigger = Triggers.BeginCombat
        triggerCondition = Conditions.Celebration
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "At the beginning of combat on your turn, if two or more nonland " +
            "permanents entered the battlefield under your control this turn, put a +1/+1 " +
            "counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "310"
        artist = "Quintin Gleim"
        flavorText = "They dash—then dine."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/5671c2a0-51e8-4d5e-8409-87abd6c0a8ab.jpg?1783915040"
    }
}

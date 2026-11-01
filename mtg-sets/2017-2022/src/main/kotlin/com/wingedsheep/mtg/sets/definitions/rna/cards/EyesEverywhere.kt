package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eyes Everywhere
 * {2}{U}
 * Enchantment
 * At the beginning of your upkeep, scry 1.
 * {5}{U}: Exchange control of this enchantment and target nonland permanent. Activate only as a sorcery.
 */
val EyesEverywhere = card("Eyes Everywhere") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, scry 1.\n" +
        "{5}{U}: Exchange control of this enchantment and target nonland permanent. Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Scry(1)
    }

    activatedAbility {
        cost = Costs.Mana("{5}{U}")
        timing = TimingRule.SorcerySpeed
        val t = target(TargetFilter.NonlandPermanent)
        effect = Effects.ExchangeControl(EffectTarget.Self, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Nils Hamm"
        flavorText = "\"They've got eyes inside my head!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1fdbf073-3ee6-402b-bd40-4781f40a6cae.jpg?1783933710"
        ruling("2019-01-25", "The effect of Eyes Everywhere's last ability lasts indefinitely. It doesn't wear off during the cleanup step, and it doesn't expire if Eyes Everywhere leaves the battlefield.")
        ruling("2019-01-25", "As Eyes Everywhere's last ability resolves, Eyes Everywhere must be on the battlefield and the target nonland permanent must be a legal target. If either of these things isn't true, the ability does nothing.")
        ruling("2019-01-25", "In a multiplayer game, if a player leaves the game, all cards that player owns leave as well. If you leave the game, the effect giving you control of the target nonland permanent ends.")
    }
}

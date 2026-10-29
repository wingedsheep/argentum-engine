package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dire Fleet Warmonger
 * {1}{B}{R}
 * Creature — Orc Pirate
 * 3/3
 *
 * At the beginning of combat on your turn, you may sacrifice another creature. If you do, this
 * creature gets +2/+2 and gains trample until end of turn.
 *
 * Ragefire Hellkite's shape: a may-pay gate over sacrificing another creature, chosen as the
 * trigger resolves, with the pump and trample as its payoff (one sacrifice at most — ruling).
 */
val DireFleetWarmonger = card("Dire Fleet Warmonger") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Orc Pirate"
    power = 3
    toughness = 3
    oracleText = "At the beginning of combat on your turn, you may sacrifice another creature. If you do, " +
        "this creature gets +2/+2 and gains trample until end of turn. (It can deal excess combat damage " +
        "to the player or planeswalker it's attacking.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.Creature, excludeSource = true),
            then = Effects.ModifyStats(2, 2, EffectTarget.Self) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self)
        )
        description = "At the beginning of combat on your turn, you may sacrifice another creature. If you do, " +
            "this creature gets +2/+2 and gains trample until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1fa781df-859f-4346-9424-b3713b17e1f6.jpg?1783930663"
        ruling("2020-06-23", "While resolving Dire Fleet Warmonger's ability, you can't sacrifice multiple creatures to give it more than +2/+2 and trample.")
        ruling("2020-06-23", "You choose whether to sacrifice a creature and which to sacrifice as Dire Fleet Warmonger's ability resolves. No player can take action between the time you make that choice and the time it gets +2/+2 and has trample, if appropriate.")
    }
}

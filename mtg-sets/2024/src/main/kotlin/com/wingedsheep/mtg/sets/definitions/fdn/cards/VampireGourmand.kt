package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vampire Gourmand
 * {1}{B}
 * Creature — Vampire
 * 2/2
 *
 * Whenever this creature attacks, you may sacrifice another creature. If you do,
 * draw a card and this creature can't be blocked this turn.
 *
 * Mirrors Beetle-Headed Merchants' pay-then-payoff: a [Effects.May] wrapping the optional
 * sacrifice of another creature you control, sequenced before the payoff so "If you do"
 * is conditional on actually sacrificing. The payoff draws a card and grants this creature
 * CANT_BE_BLOCKED until end of turn ([EffectTarget.Self], default end-of-turn duration).
 */
val VampireGourmand = card("Vampire Gourmand") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature attacks, you may sacrifice another creature. If you do, " +
        "draw a card and this creature can't be blocked this turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.Creature, excludeSource = true),
            then = Effects.DrawCards(1) then
                Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        )
        description = "Whenever this creature attacks, you may sacrifice another creature. If you do, " +
            "draw a card and this creature can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Chris Rallis"
        flavorText = "\"He asked me if I wanted to come in for a bite to eat. Who am I to turn down a stranger's kindness?\""
        imageUri = "https://cards.scryfall.io/normal/front/9/1/917514c0-9cd5-4b97-85b9-c4f753560ad4.jpg?1782689202"
    }
}

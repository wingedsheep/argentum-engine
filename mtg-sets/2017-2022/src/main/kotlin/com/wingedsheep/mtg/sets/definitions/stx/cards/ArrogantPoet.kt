package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arrogant Poet — Strixhaven: School of Mages #63 (canonical printing)
 * {1}{B} · Creature — Human Warlock · 2/1
 *
 * Whenever this creature attacks, you may pay 2 life. If you do, it gains flying until end of turn.
 *
 * Order of the Golden Cricket's shape with a life payment: [Effects.MayPay] over
 * [Effects.PayLife], gating an end-of-turn flying grant. The choice is made on resolution.
 */
val ArrogantPoet = card("Arrogant Poet") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    oracleText = "Whenever this creature attacks, you may pay 2 life. If you do, it gains flying until end of turn."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            cost = Effects.PayLife(2),
            then = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Billy Christian"
        flavorText = "\"I would be kinder to my peers, Professor, but I simply don't have any.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/5/556a0816-83c5-41dc-8546-213b21e2cceb.jpg?1783927370"
        ruling("4/16/2021", "You choose whether or not to pay 2 life as the ability resolves. Once you make that choice, no one can respond before Arrogant Poet gains flying.")
    }
}

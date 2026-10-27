package com.wingedsheep.mtg.sets.definitions.frc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Niv-Mizzet, Ghost Counsel — Reality Fracture Commander #10
 * {2}{W}{W}{B}{B} · Legendary Creature — Spirit Dragon · 4/4
 *
 * Flying
 * Whenever you gain life, you may pay that much life. If you do, draw that many cards.
 * {T}: Each opponent loses 1 life and you gain 1 life.
 *
 * "Pay that much life" is a [Effects.MayPay] gate over [Effects.PayDynamicLife] reading the
 * triggering event's amount; the gate only offers the payment while your life total covers it
 * (CR 119.4). The tap ability gains a printed 1, not "the life lost this way", so it is a plain
 * `LoseLife then GainLife` rather than a drain.
 */
val NivMizzetGhostCounsel = card("Niv-Mizzet, Ghost Counsel") {
    manaCost = "{2}{W}{W}{B}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Spirit Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever you gain life, you may pay that much life. If you do, draw that many cards.\n" +
        "{T}: Each opponent loses 1 life and you gain 1 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.MayPay(
            cost = Effects.PayDynamicLife(DynamicAmounts.triggerLifeGained()),
            then = Effects.DrawCards(DynamicAmounts.triggerLifeGained()),
        )
        description = "Whenever you gain life, you may pay that much life. If you do, draw that many cards."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
        description = "{T}: Each opponent loses 1 life and you gain 1 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Brian Valeza"
        flavorText = "Devourer of the Obzedat, tithe-drinker, and necroparun eternal."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0ad0e168-ac5c-4e1b-83f2-0488067a58d7.jpg?1789644348"
        ruling(
            "2026-08-21",
            "If more than one creature you control with lifelink deals combat damage at the same time, " +
                "Niv-Mizzet's triggered ability will trigger once for each of those creatures."
        )
    }
}

package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heavyweight Demolisher — The Brothers' War #160
 * {7} · Artifact Creature — Construct · Uncommon
 * 8/6
 *
 * Menace
 * At the beginning of your upkeep, tap this creature unless you pay {3}.
 * Unearth {6}{R}{R}
 *
 * The upkeep tax is a resolution-time [Effects.PayOrSuffer] over {3}, the same shape as
 * Electrozoa's energy tax.
 */
val HeavyweightDemolisher = card("Heavyweight Demolisher") {
    manaCost = "{7}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Construct"
    power = 8
    toughness = 6
    oracleText = "Menace\n" +
        "At the beginning of your upkeep, tap this creature unless you pay {3}.\n" +
        "Unearth {6}{R}{R} ({6}{R}{R}: Return this card from your graveyard to the battlefield. It gains haste. " +
        "Exile it at the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Mana("{3}"),
            suffer = Effects.Tap(EffectTarget.Self),
        )
    }

    unearth("{6}{R}{R}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f72de800-b28e-42c4-a3ca-5ec3a765d807.jpg?1783920056"
    }
}

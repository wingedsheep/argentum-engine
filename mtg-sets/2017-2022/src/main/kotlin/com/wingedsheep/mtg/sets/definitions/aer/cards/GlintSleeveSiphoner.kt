package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glint-Sleeve Siphoner
 * {1}{B}
 * Creature — Human Rogue
 * 2/1
 * Menace
 * Whenever this creature enters or attacks, you get {E} (an energy counter).
 * At the beginning of your upkeep, you may pay {E}{E}. If you do, you draw a card and you lose 1 life.
 *
 * "Enters or attacks" is two trigger events on one printed line, so it is authored as two triggered
 * abilities sharing one effect (the Sun Titan / Skysovereign shape) — each fires independently.
 */
val GlintSleeveSiphoner = card("Glint-Sleeve Siphoner") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 1
    oracleText = "Menace\n" +
        "Whenever this creature enters or attacks, you get {E} (an energy counter).\n" +
        "At the beginning of your upkeep, you may pay {E}{E}. If you do, you draw a card and you lose 1 life."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(1)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.GetEnergy(1)
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 2),
            then = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Greg Opalinski"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/315976db-4cab-4393-8386-ce3b0ae3f490.jpg?1783936762"
        ruling(
            "2017-02-09",
            "The first triggered ability triggers both when Glint-Sleeve Siphoner enters the battlefield and " +
                "whenever it attacks. You don't have to choose only one."
        )
        ruling(
            "2024-06-07",
            "Some triggered abilities state that you \"may pay\" a certain amount of {E}. You can't pay that " +
                "amount multiple times to multiply the effect. You simply choose whether or not to pay that " +
                "amount of {E} as the ability resolves."
        )
    }
}

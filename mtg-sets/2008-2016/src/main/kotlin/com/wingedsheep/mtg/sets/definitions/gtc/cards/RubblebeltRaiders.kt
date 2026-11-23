package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rubblebelt Raiders — Gatecrash #224 (canonical / earliest real printing)
 * {1}{R/G}{R/G}{R/G} · Creature — Human Warrior · 3/3
 *
 * Whenever this creature attacks, put a +1/+1 counter on it for each attacking creature you control.
 *
 * Akroan Hoplite's count (attacking creatures you control, read on resolution, so it includes
 * itself) feeding permanent +1/+1 counters instead of an until-end-of-turn pump.
 */
val RubblebeltRaiders = card("Rubblebelt Raiders") {
    manaCost = "{1}{R/G}{R/G}{R/G}"
    colorIdentity = "RG"
    typeLine = "Creature — Human Warrior"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature attacks, put a +1/+1 counter on it for each attacking creature you control."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.attackingCreaturesYouControl(),
            target = EffectTarget.Self,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Chippy"
        flavorText = "\"This city will perish, and the Gruul will cheer as the boar-god crushes the last bricks into dust.\"\n—Nikya of the Old Ways"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2dec7d6a-2362-4c62-bd81-35bba6086f7d.jpg"
    }
}

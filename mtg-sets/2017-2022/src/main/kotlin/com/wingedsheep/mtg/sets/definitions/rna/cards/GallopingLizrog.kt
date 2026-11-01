package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Galloping Lizrog
 * {3}{G}{U}
 * Creature — Frog Lizard
 * 3/3
 * Trample
 * When this creature enters, you may remove any number of +1/+1 counters from among creatures you
 * control. If you do, put twice that many +1/+1 counters on this creature.
 *
 * "From among creatures you control" includes the Lizrog itself. The removal is
 * `RemoveCountersFromAmong`, which records the total under "removed"; doubling it onto the Lizrog
 * places nothing when none were removed, which is the "if you do".
 */
val GallopingLizrog = card("Galloping Lizrog") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Frog Lizard"
    oracleText = "Trample\nWhen this creature enters, you may remove any number of +1/+1 counters from " +
        "among creatures you control. If you do, put twice that many +1/+1 counters on this creature."
    power = 3
    toughness = 3
    keywords(Keyword.TRAMPLE)
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.RemoveCountersFromAmong(
            CounterType.PLUS_ONE_PLUS_ONE,
            GameObjectFilter.Creature.youControl(),
            storeAmountAs = "removed"
        ) then Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.storedNumber("removed") * 2,
            EffectTarget.Self
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Lius Lasahido"
        flavorText = "\"I might have overfed him.\"\n—Gulistan, Simic biomancer"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc2b7cfe-4696-4448-93d2-33be596e32d9.jpg?1783933649"
    }
}

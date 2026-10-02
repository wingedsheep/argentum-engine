package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scurry of Gremlins
 * {2}{R}{W}
 * Enchantment
 * When this enchantment enters, create two 1/1 red Gremlin creature tokens. Then you get an
 * amount of {E} (energy counters) equal to the number of creatures you control.
 * Pay {E}{E}{E}{E}: Creatures you control get +1/+0 and gain haste until end of turn.
 *
 * The energy count is read after the tokens exist ("Then"), so the two fresh Gremlins count.
 */
val ScurryOfGremlins = card("Scurry of Gremlins") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, create two 1/1 red Gremlin creature tokens. Then you " +
        "get an amount of {E} (energy counters) equal to the number of creatures you control.\n" +
        "Pay {E}{E}{E}{E}: Creatures you control get +1/+0 and gain haste until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Gremlin"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/1/a/1af08a84-4a57-4c94-a290-31d93f79db83.jpg?1783911112"
        ) then Effects.AddDynamicCounters(
            CounterType.ENERGY,
            DynamicAmounts.creaturesYouControl(),
            EffectTarget.Controller
        )
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 4)
        effect = Patterns.Group.pumpAndGrantToAll(1, 0, Keyword.HASTE, Filters.Group.creaturesYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Ben Wootten"
        flavorText = "A whiff of aether brings the horde out of hiding."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/470ce8e9-64b9-403c-bee0-66a4c47ca6d4.jpg?1783911247"
    }
}

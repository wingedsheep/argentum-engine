package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AmplifyEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Kilnmouth Dragon
 * {5}{R}{R}
 * Creature — Dragon
 * 5/5
 * Amplify 3 (As this creature enters, put three +1/+1 counters on it for each
 * Dragon card you reveal in your hand.)
 * Flying
 * {T}: Kilnmouth Dragon deals damage equal to the number of +1/+1 counters on
 * it to any target.
 */
val KilnmouthDragon = card("Kilnmouth Dragon") {
    manaCost = "{5}{R}{R}"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Amplify 3 (As this creature enters, put three +1/+1 counters on it for each Dragon card you reveal in your hand.)\nFlying\n{T}: Kilnmouth Dragon deals damage equal to the number of +1/+1 counters on it to any target."

    keywords(Keyword.AMPLIFY, Keyword.FLYING)

    replacementEffect(AmplifyEffect(countersPerReveal = 3))

    activatedAbility {
        cost = Costs.Tap
        val t = target("any target", Targets.Any)
        effect = DealDamageEffect(
            amount = DynamicAmount.CountersOnSelf(CounterTypeFilter.PlusOnePlusOne),
            target = t
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "104"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/effe13c3-3c8b-4faa-bdd4-491039bfa82b.jpg?1562943198"
    }
}

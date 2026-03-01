package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AmplifyEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Daru Stinger
 * {3}{W}
 * Creature — Human Soldier
 * 1/1
 * Amplify 1 (As this creature enters, put a +1/+1 counter on it for each
 * Soldier card you reveal in your hand.)
 * {T}: Daru Stinger deals damage equal to the number of +1/+1 counters on
 * it to target attacking or blocking creature.
 */
val DaruStinger = card("Daru Stinger") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Amplify 1 (As this creature enters, put a +1/+1 counter on it for each Soldier card you reveal in your hand.)\n{T}: Daru Stinger deals damage equal to the number of +1/+1 counters on it to target attacking or blocking creature."

    keywords(Keyword.AMPLIFY)

    replacementEffect(AmplifyEffect(countersPerReveal = 1))

    activatedAbility {
        cost = Costs.Tap
        val t = target("target", TargetCreature(filter = TargetFilter.AttackingOrBlockingCreature))
        effect = DealDamageEffect(
            amount = DynamicAmount.CountersOnSelf(CounterTypeFilter.PlusOnePlusOne),
            target = t
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff5866a4-f4c0-45bc-9b33-b77387441d34.jpg?1562946568"
    }
}

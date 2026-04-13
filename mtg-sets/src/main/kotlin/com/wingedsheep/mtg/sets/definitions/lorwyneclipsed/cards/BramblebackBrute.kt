package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CantBlockTargetCreaturesEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Brambleback Brute
 * {2}{R}
 * Creature — Giant Warrior
 * 4/5
 *
 * This creature enters with two -1/-1 counters on it.
 * {1}{R}, Remove a counter from this creature: Target creature can't block this turn.
 * Activate only as a sorcery.
 */
val BramblebackBrute = card("Brambleback Brute") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Giant Warrior"
    power = 4
    toughness = 5
    oracleText = "This creature enters with two -1/-1 counters on it.\n{1}{R}, Remove a counter from this creature: Target creature can't block this turn. Activate only as a sorcery."

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.MinusOneMinusOne,
        count = 2,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R}"),
            Costs.RemoveCounterFromSelf(Counters.MINUS_ONE_MINUS_ONE)
        )
        val creature = target("creature", Targets.Creature)
        effect = CantBlockTargetCreaturesEffect()
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Aaron Miller"
        flavorText = "Her tangled cape grew with every village razed."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5ebb8365-c6e1-46e8-a242-6aa27b21e68a.jpg?1767952109"
    }
}

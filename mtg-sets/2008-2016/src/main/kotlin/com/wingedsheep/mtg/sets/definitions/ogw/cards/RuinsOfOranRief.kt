package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Ruins of Oran-Rief — Oath of the Gatewatch #176 (canonical printing)
 * Land
 *
 * This land enters tapped.
 * {T}: Add {C}.
 * {T}: Put a +1/+1 counter on target colorless creature that entered this turn.
 *
 * The targeted, colorless counterpart of Oran-Rief, the Vastwood. The target is any controller's
 * creature; colorlessness is read off projected colours when the target is chosen and rechecked on
 * resolution, while "entered this turn" only asks when it entered (2016-01-22 ruling).
 */
val RuinsOfOranRief = card("Ruins of Oran-Rief") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {C}. ({C} represents colorless mana.)\n" +
        "{T}: Put a +1/+1 counter on target colorless creature that entered this turn."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}."
    }

    activatedAbility {
        cost = Costs.Tap
        val creature = target(
            TargetFilter(
                GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless).enteredThisTurn()
            )
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "{T}: Put a +1/+1 counter on target colorless creature that entered this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fed86e5-5631-401b-8820-9685f8296134.jpg?1783937891"
        ruling(
            "2016-01-22",
            "The target of the last ability didn't necessarily have to be a colorless creature as it entered " +
                "the battlefield, provided it entered the battlefield during that turn. However, it does have " +
                "to be a colorless creature to be a legal target of the ability."
        )
    }
}

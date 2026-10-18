package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MaxCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rasputin Dreamweaver
 * {4}{W}{U}
 * Legendary Creature — Human Wizard
 * 4/1
 *
 * Rasputin enters with seven dream counters on it.
 * Remove a dream counter from Rasputin: Add {C}.
 * Remove a dream counter from Rasputin: Prevent the next 1 damage that would be dealt to Rasputin this turn.
 * At the beginning of your upkeep, if Rasputin started the turn untapped, put a dream counter on it.
 * Rasputin can't have more than seven dream counters on it.
 *
 * The upkeep refill's intervening "if" reads the turn-start snapshot
 * (`StatePredicate.StartedTurnUntapped`), so a Rasputin that was tapped going into the turn gets
 * nothing even though its untap step has untapped it by the upkeep. [MaxCounters] is the limit
 * every counter placement honours, its entry counters included.
 */
val RasputinDreamweaver = card("Rasputin Dreamweaver") {
    manaCost = "{4}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Wizard"
    power = 4
    toughness = 1
    oracleText = "Rasputin enters with seven dream counters on it.\n" +
        "Remove a dream counter from Rasputin: Add {C}.\n" +
        "Remove a dream counter from Rasputin: Prevent the next 1 damage that would be dealt to Rasputin this turn.\n" +
        "At the beginning of your upkeep, if Rasputin started the turn untapped, put a dream counter on it.\n" +
        "Rasputin can't have more than seven dream counters on it."

    replacementEffect(EntersWithCounters(counterType = CounterType.DREAM, count = 7, selfOnly = true))

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.DREAM, 1)
        manaAbility = true
        effect = Effects.AddColorlessMana(1)
        description = "Remove a dream counter from Rasputin: Add {C}."
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.DREAM, 1)
        effect = Effects.PreventNextDamage(1, EffectTarget.Self)
        description = "Remove a dream counter from Rasputin: Prevent the next 1 damage that would be dealt to Rasputin this turn."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.SourceMatches(GameObjectFilter.Permanent.startedTurnUntapped())
        effect = Effects.AddCounters(CounterType.DREAM, 1, EffectTarget.Self)
    }

    staticAbility {
        ability = MaxCounters(CounterType.DREAM, 7)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "253"
        artist = "Andi Rusu"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/503256f8-3aab-49d0-b78b-6502aa29ce52.jpg?1783948034"
    }
}

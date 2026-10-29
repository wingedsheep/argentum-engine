package com.wingedsheep.mtg.sets.definitions.c15.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Karlov of the Ghost Council
 * {W}{B}
 * Legendary Creature — Spirit Advisor
 * 2/2
 * Whenever you gain life, put two +1/+1 counters on Karlov.
 * {W}{B}, Remove six +1/+1 counters from Karlov: Exile target creature.
 */
val KarlovOfTheGhostCouncil = card("Karlov of the Ghost Council") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Spirit Advisor"
    power = 2
    toughness = 2
    oracleText = "Whenever you gain life, put two +1/+1 counters on Karlov.\n" +
        "{W}{B}, Remove six +1/+1 counters from Karlov: Exile target creature."

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        description = "Whenever you gain life, put two +1/+1 counters on Karlov."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{W}{B}"),
            Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE, 6),
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.Exile(creature)
        description = "{W}{B}, Remove six +1/+1 counters from Karlov: Exile target creature."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "46"
        artist = "Volkan Baǵa"
        flavorText = "\"Does Teysa think me blind to her scheming? Ambition runs in our family.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8cf01688-7046-4242-8647-d1c226ae53f7.jpg?1783938107"
        ruling("2015-11-04", "The ability triggers just once for each life-gaining event, whether it's 1 life from an attacking creature with lifelink or 4 life from Faith's Fetters.")
    }
}

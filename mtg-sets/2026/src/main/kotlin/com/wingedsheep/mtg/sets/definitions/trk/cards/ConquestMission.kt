package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Conquest Mission
 * {2}{W}
 * Enchantment
 *
 * Whenever a nontoken artifact you control enters, put a quest counter on this enchantment.
 * Create a 1/1 colorless Borg artifact creature token. If this enchantment has six or more quest
 * counters on it, create two of those tokens instead.
 *
 * The token count is read after the counter lands, so the sixth trigger already makes two.
 */
val ConquestMission = card("Conquest Mission") {
    manaCost = "{2}{W}"
    typeLine = "Enchantment"
    colorIdentity = "W"
    oracleText = "Whenever a nontoken artifact you control enters, put a quest counter on this enchantment. " +
        "Create a 1/1 colorless Borg artifact creature token. If this enchantment has six or more quest " +
        "counters on it, create two of those tokens instead."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.nontoken().youControl()).enters()
        effect = Effects.AddCounters(CounterType.QUEST, 1, EffectTarget.Self) then
            Effects.CreateToken(
                count = DynamicAmounts.conditional(
                    Conditions.CompareAmounts(
                        DynamicAmounts.countersOnSelf(CounterType.QUEST),
                        ComparisonOperator.GTE,
                        6
                    ),
                    2,
                    1
                ),
                power = 1, toughness = 1,
                creatureTypes = setOf("Borg"), artifactToken = true
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Mark Spears"
        flavorText = "The iron hand of mercy rendered grief, remorse, loss, and fear all irrelevant."
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f91755d7-6fb9-4caf-b4b4-f21ed5572d52.jpg?1792750872"
    }
}

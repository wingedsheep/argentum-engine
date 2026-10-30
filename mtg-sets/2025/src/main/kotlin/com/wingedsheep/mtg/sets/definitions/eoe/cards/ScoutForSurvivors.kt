package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TotalCap

/**
 * Scout for Survivors {2}{W}
 * Sorcery
 *
 * Return up to three target creature cards with total mana value 3 or less
 * from your graveyard to the battlefield. Put a +1/+1 counter on each of them.
 *
 * The creature cards are real targets chosen as the spell is cast (CR 601.2c), with the summed
 * mana value capped by [TargetObject.totalAtMost]. Any target that has left the graveyard
 * by resolution is dropped (CR 608.2b); the rest come back with their counter.
 */
val ScoutForSurvivors = card("Scout for Survivors") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return up to three target creature cards with total mana value 3 or less from your graveyard to the battlefield. Put a +1/+1 counter on each of them."

    spell {
        targets(
            TargetFilter.CreatureInYourGraveyard,
            count = 3,
            optional = true,
            totalAtMost = TotalCap.manaValue(DynamicAmounts.fixed(3)),
        )
        effect = Effects.Pipeline {
            val survivors = gather(CardSource.ChosenTargets)
            move(survivors, CardDestination.ToZone(Zone.BATTLEFIELD), addCounterType = CounterType.PLUS_ONE_PLUS_ONE)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Greg Staples"
        flavorText = "\"We've received transmission! They're alive—just barely.\"\n—Sunstar Free Company Mission Control"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebf3a6dd-a447-46f9-8b10-091ac8cbaa18.jpg?1752946680"
        ruling("2025-07-25", "If a card in your graveyard has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
    }
}

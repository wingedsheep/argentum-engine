package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Mindwhisker
 * {2}{U}
 * Creature — Rat Wizard
 * 3/2
 *
 * At the beginning of your upkeep, surveil 1.
 * Threshold — As long as there are seven or more cards in your graveyard,
 * creatures your opponents control get -1/-0.
 */
val Mindwhisker = card("Mindwhisker") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Rat Wizard"
    power = 3
    toughness = 2
    oracleText = "At the beginning of your upkeep, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)\nThreshold — As long as there are seven or more cards in your graveyard, creatures your opponents control get -1/-0."

    // At the beginning of your upkeep, surveil 1
    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = EffectPatterns.surveil(1)
    }

    // Threshold: creatures your opponents control get -1/-0
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = -1,
                toughnessBonus = 0,
                filter = GroupFilter.AllCreaturesOpponentsControl
            ),
            condition = Conditions.CardsInGraveyardAtLeast(7)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aaa10f34-5bfd-4d87-8f07-58de3b0f5663.jpg?1721426158"
    }
}

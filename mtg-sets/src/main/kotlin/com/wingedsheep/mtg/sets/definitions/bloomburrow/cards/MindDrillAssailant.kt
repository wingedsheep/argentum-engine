package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StaticTarget

/**
 * Mind Drill Assailant
 * {2}{U/B}{U/B}
 * Creature — Rat Warlock
 * 2/5
 *
 * Threshold — As long as there are seven or more cards in your graveyard,
 * this creature gets +3/+0.
 * {2}{U/B}: Surveil 1.
 */
val MindDrillAssailant = card("Mind Drill Assailant") {
    manaCost = "{2}{U/B}{U/B}"
    typeLine = "Creature — Rat Warlock"
    power = 2
    toughness = 5
    oracleText = "Threshold — As long as there are seven or more cards in your graveyard, this creature gets +3/+0.\n{2}{U/B}: Surveil 1."

    // Threshold: +3/+0
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(3, 0, StaticTarget.SourceCreature),
            condition = Conditions.CardsInGraveyardAtLeast(7)
        )
    }

    // {2}{U/B}: Surveil 1
    activatedAbility {
        cost = Costs.Mana("{2}{U/B}")
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "225"
        artist = "Tuan Duong Chu"
        flavorText = "\"If only you kept your secrets somewhere more secure than your delicate little skull.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/0/507ba708-ca9b-453e-b4c2-23b6650eb5a8.jpg?1721427150"
    }
}

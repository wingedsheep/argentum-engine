package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thud
 * {R}
 * Sorcery
 *
 * As an additional cost to cast this spell, sacrifice a creature.
 * Thud deals damage equal to the sacrificed creature's power to any target.
 */
val Thud = card("Thud") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\nThud deals damage equal to the sacrificed creature's power to any target."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        val anyTarget = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.sacrificedPower(), anyTarget)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Kev Walker"
        flavorText = "\"Give 'em a push.\"\n—Mogwin, goblin delinquent"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11bed78a-c579-424a-8fcc-322ce2630dbc.jpg?1783934543"
        ruling("2018-07-13", "The sacrificed creature's last known existence on the battlefield is checked to determine its power.")
        ruling("2018-07-13", "You must sacrifice exactly one creature to cast Thud; you can't cast it without sacrificing a creature, and you can't sacrifice additional creatures.")
    }
}

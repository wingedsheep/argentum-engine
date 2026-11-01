package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spark Harvest — War of the Spark #105 (canonical printing)
 * {B} · Sorcery
 *
 * As an additional cost to cast this spell, sacrifice a creature or pay {3}{B}.
 * Destroy target creature or planeswalker.
 *
 * The "sacrifice a creature or pay {3}{B}" choice is [Costs.additional.SacrificeOrPay]: the caster
 * picks one leg at cast time, the pay leg adding {3}{B} on top of the base {B}.
 */
val SparkHarvest = card("Spark Harvest") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature or pay {3}{B}.\n" +
        "Destroy target creature or planeswalker."

    additionalCost(
        Costs.additional.SacrificeOrPay(
            filter = GameObjectFilter.Creature,
            alternativeManaCost = "{3}{B}",
        )
    )

    spell {
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker))
        effect = Effects.Destroy(victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Kieran Yanner"
        flavorText = "With the harvest of Domri's spark, the Elderspell began to fuel Bolas's ascension to godhood."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2013a138-f8e2-4a67-91e8-759288d985a7.jpg"
    }
}

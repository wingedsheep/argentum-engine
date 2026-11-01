package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Solar Blaze — War of the Spark #216 (canonical printing)
 * {2}{R}{W}
 * Sorcery
 * Each creature deals damage to itself equal to its power.
 *
 * Walks every creature on the battlefield, each the source of its own damage — the same shape as
 * The Akroan War's chapter III, without the tapped restriction. Because each creature is the
 * source, its own lifelink, deathtouch, infect and wither apply to the damage it deals itself.
 */
val SolarBlaze = card("Solar Blaze") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Sorcery"
    oracleText = "Each creature deals damage to itself equal to its power."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreatures,
            Effects.DealDamage(
                amount = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                target = EffectTarget.IterationEntity,
                damageSource = EffectTarget.IterationEntity,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "216"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb72ba0f-ab3a-41e6-906d-a84039efa0af.jpg?1783933389"
    }
}

package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Spark Trooper
 * {1}{R}{R}{W}
 * Creature — Elemental Soldier
 * 6/1
 * Trample, lifelink, haste
 * At the beginning of the end step, sacrifice this creature.
 */
val SparkTrooper = card("Spark Trooper") {
    manaCost = "{1}{R}{R}{W}"
    colorIdentity = "WR"
    typeLine = "Creature — Elemental Soldier"
    oracleText = "Trample, lifelink, haste\nAt the beginning of the end step, sacrifice this creature."
    power = 6
    toughness = 1
    keywords(Keyword.TRAMPLE, Keyword.LIFELINK, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = SacrificeSelfEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "199"
        artist = "James Ryman"
        flavorText = "Izzet-designed conductors atop Sunhome charge empty sets of armor that give form to the fluctuating elementals."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09eb69b5-b8e2-48c6-8c27-cb0108df8dad.jpg"
    }
}

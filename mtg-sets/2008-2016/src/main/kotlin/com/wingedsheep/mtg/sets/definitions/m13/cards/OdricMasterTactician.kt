package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * Odric, Master Tactician
 * {2}{W}{W}
 * Legendary Creature — Human Soldier
 * 3/4
 * First strike
 * Whenever Odric and at least three other creatures attack, you choose which creatures block this
 * combat and how those creatures block.
 */
val OdricMasterTactician = card("Odric, Master Tactician") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier"
    power = 3
    toughness = 4
    oracleText = "First strike (This creature deals combat damage before creatures without first strike.)\n" +
        "Whenever Odric and at least three other creatures attack, you choose which creatures block this " +
        "combat and how those creatures block."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(4)))
        effect = Effects.ChooseBlockersThisCombat()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Michael Komarck"
        flavorText = "\"Fear holds no place in faith's battle plan.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13bfcec9-a789-41f1-aa3d-4e4cdd7555b2.jpg?1783915716"
    }
}

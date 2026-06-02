package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Wary Farmer
 * {1}{G/W}{G/W}
 * Creature — Kithkin Citizen
 * 3/3
 * At the beginning of your end step, if another creature entered the battlefield under your
 * control this turn, surveil 1.
 */
val WaryFarmer = card("Wary Farmer") {
    manaCost = "{1}{G/W}{G/W}"
    colorIdentity = "WG"
    typeLine = "Creature — Kithkin Citizen"
    power = 3
    toughness = 3
    oracleText = "At the beginning of your end step, if another creature entered the battlefield under your control this turn, surveil 1."

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Exists(
            player = Player.You,
            zone = Zone.BATTLEFIELD,
            filter = GameObjectFilter.Creature
                .enteredThisTurn()
                .youControl(),
            excludeSelf = true
        )
        effect = LibraryPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "251"
        artist = "Ron Spears"
        flavorText = "\"The only 'farmhands' I need are my own.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22d20c0d-176d-49c9-aa0b-2c5778548cc5.jpg?1767749680"
    }
}

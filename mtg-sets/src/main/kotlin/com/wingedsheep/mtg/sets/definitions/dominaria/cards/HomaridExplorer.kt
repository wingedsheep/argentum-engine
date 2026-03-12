package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Homarid Explorer
 * {3}{U}
 * Creature — Homarid Scout
 * 3/3
 * When this creature enters, target player mills four cards.
 */
val HomaridExplorer = card("Homarid Explorer") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Homarid Scout"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, target player mills four cards."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", Targets.Player)
        effect = EffectPatterns.mill(4, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Jehan Choo"
        flavorText = "\"Homarids spread northward from Sarpadia as the climate cooled, raiding coastal settlements for supplies.\" —Time of Ice"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93be7aa6-1897-4657-b3a6-991b05f7ea5e.jpg?1593860681"
    }
}

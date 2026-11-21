package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ubul Sar Gatekeepers
 * {3}{B}
 * Creature — Zombie Soldier
 * 2/4
 * When this creature enters, if you control two or more Gates, target creature an opponent controls
 * gets -2/-2 until end of turn.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val UbulSarGatekeepers = card("Ubul Sar Gatekeepers") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Soldier"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, if you control two or more Gates, target creature an opponent " +
        "controls gets -2/-2 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControlAtLeast(2, GameObjectFilter.Land.withSubtype("Gate"))
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ModifyStats(-2, -2, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Volkan Baǵa"
        flavorText = "There are many gates into the undercity, but few ways out."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5b2e327-adfd-459b-8d18-faa39d88b5de.jpg?1783940038"
    }
}

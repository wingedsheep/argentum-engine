package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cyclops Electromancer
 * {4}{R}
 * Creature — Cyclops Wizard
 * 4/2
 * When this creature enters, it deals X damage to target creature an opponent controls,
 * where X is the number of instant and sorcery cards in your graveyard.
 */
val CyclopsElectromancer = card("Cyclops Electromancer") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Cyclops Wizard"
    power = 4
    toughness = 2
    oracleText = "When this creature enters, it deals X damage to target creature an opponent controls, where X is the number of instant and sorcery cards in your graveyard."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.DealDamage(
            DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery),
            creature
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Jason Felix"
        flavorText = "Every storm has an eye."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da0966a6-f378-4975-88ae-23b600d578bf.jpg?1783933430"
        ruling("2019-05-03", "The number of instant and sorcery cards in your graveyard is counted only as Cyclops Electromancer's ability resolves.")
        ruling("2019-05-03", "A split card that's both an instant and a sorcery is counted only once for Cyclops Electromancer's ability.")
    }
}

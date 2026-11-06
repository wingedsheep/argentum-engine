package com.wingedsheep.mtg.sets.definitions.akh.cards

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
 * Warfire Javelineer
 * {3}{R}
 * Creature — Minotaur Warrior
 * 2/3
 * When this creature enters, it deals X damage to target creature an opponent controls, where X
 * is the number of instant and sorcery cards in your graveyard.
 *
 * X is counted on resolution; a card that is both an instant and a sorcery counts once, since
 * the count is over cards matching [GameObjectFilter.InstantOrSorcery].
 */
val WarfireJavelineer = card("Warfire Javelineer") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Warrior"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, it deals X damage to target creature an opponent controls, " +
        "where X is the number of instant and sorcery cards in your graveyard."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val theirs = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.DealDamage(
            DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery),
            theirs
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Yohann Schepacz"
        flavorText = "Minotaurs favor magic that manifests as weapons wrapped in flame."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5ce75b3c-4f83-4c09-9361-d7463e4921e6.jpg?1783936481"
        ruling("2017-04-18", "A split card only counts once for Warfire Javelineer's ability, even if it's both an instant and a sorcery.")
    }
}

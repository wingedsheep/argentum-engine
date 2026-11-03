package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Vraska's Conquistador
 * {1}{B}
 * Creature — Vampire Soldier
 * 2/1
 *
 * Whenever this creature attacks or blocks, if you control a Vraska planeswalker, target opponent
 * loses 2 life and you gain 2 life.
 *
 * "Attacks or blocks" is two triggered abilities sharing the intervening-if (CR 603.4).
 */
val VraskasConquistador = card("Vraska's Conquistador") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Soldier"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature attacks or blocks, if you control a Vraska planeswalker, " +
        "target opponent loses 2 life and you gain 2 life."

    val controlsVraska = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Vraska"))

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = controlsVraska
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(2, opponent) then Effects.GainLife(2)
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        interveningIf = controlsVraska
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(2, opponent) then Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Sidharth Chaturvedi"
        flavorText = "Abandoning both queen and church, he chose to follow power."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d890a17-ffd4-4bda-bef3-2c116f7bf66f.jpg?1783935259"
    }
}

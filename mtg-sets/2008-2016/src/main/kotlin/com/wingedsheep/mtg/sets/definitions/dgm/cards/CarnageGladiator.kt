package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Carnage Gladiator
 * {2}{B}{R}
 * Creature — Skeleton Warrior
 * 4/2
 *
 * Whenever a creature blocks, that creature's controller loses 1 life.
 * {1}{B}{R}: Regenerate this creature.
 *
 * The trigger is `Triggers.a(Creature).blocks()` — one trigger per declared blocker, whoever controls
 * it and however many attackers it blocks — with [EffectTarget.ControllerOfTriggeringEntity] naming
 * the blocker's controller (Mage Hunters' Onslaught's rider, as a permanent's ability).
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val CarnageGladiator = card("Carnage Gladiator") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Skeleton Warrior"
    power = 4
    toughness = 2
    oracleText = "Whenever a creature blocks, that creature's controller loses 1 life.\n" +
        "{1}{B}{R}: Regenerate this creature."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).blocks()
        effect = Effects.LoseLife(1, EffectTarget.ControllerOfTriggeringEntity)
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}{R}")
        effect = Effects.Regenerate(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Ryan Barger"
        flavorText = "Not the foulest or bloodiest act at Rix Maadi by a stretch."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20bde6c1-917c-4860-a8d0-a9d7c461f8d2.jpg?1783940031"
        ruling("2017-03-14", "Carnage Gladiator's ability triggers whenever any creature blocks, regardless of who controls that creature or which creature it blocked.")
        ruling("2017-03-14", "If a creature can block multiple creatures and does so, Carnage Gladiator's ability triggers only once.")
    }
}

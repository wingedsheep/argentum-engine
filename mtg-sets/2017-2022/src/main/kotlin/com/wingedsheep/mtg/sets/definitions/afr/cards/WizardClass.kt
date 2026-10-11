package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wizard Class {U}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * You have no maximum hand size.
 *
 * {2}{U}: Level 2
 * When this Class becomes level 2, draw two cards.
 *
 * {4}{U}: Level 3
 * Whenever you draw a card, put a +1/+1 counter on target creature you control.
 */
val WizardClass = card("Wizard Class") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "You have no maximum hand size.\n" +
        "{2}{U}: Level 2 — When this Class becomes level 2, draw two cards.\n" +
        "{4}{U}: Level 3 — Whenever you draw a card, put a +1/+1 counter on target creature you control."

    // Level 1: no maximum hand size.
    staticAbility {
        ability = NoMaximumHandSize
    }

    // Level 2: when this Class becomes level 2, draw two cards.
    classLevel(2, "{2}{U}") {
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.DrawCards(2)
            description = "When this Class becomes level 2, draw two cards."
        }
    }

    // Level 3: each card you draw puts a +1/+1 counter on target creature you control.
    classLevel(3, "{4}{U}") {
        triggeredAbility {
            trigger = Triggers.you.draws()
            val creature = target(TargetFilter.Creature.youControl())
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
            description = "Whenever you draw a card, put a +1/+1 counter on target creature you control."
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Marta Nael"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1f629fb-b097-4240-8560-ef47f5678f48.jpg?1783926504"
        ruling("2021-07-23", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2021-07-23", "Gaining a level won't remove abilities that a Class had at a previous level.")
        ruling("2021-07-23", "You can't activate the first level ability of a Class unless that Class is level 1. Similarly, you can't activate the second level ability of a Class unless that Class is level 2.")
    }
}

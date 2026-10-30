package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Titans' Nest — Ikoria: Lair of Behemoths #212 (canonical printing)
 * {1}{B}{G}{U}
 * Enchantment
 *
 * At the beginning of your upkeep, surveil 1.
 * Exile a card from your graveyard: Add {C}. Spend this mana only to cast a spell that's one or
 * more colors without {X} in its mana cost.
 */
val TitansNest = card("Titans' Nest") {
    manaCost = "{1}{B}{G}{U}"
    colorIdentity = "BGU"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, surveil 1. (Look at the top card of your library. You may put that card into your graveyard.)\n" +
        "Exile a card from your graveyard: Add {C}. Spend this mana only to cast a spell that's one or more colors without {X} in its mana cost."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Surveil(1)
    }

    activatedAbility {
        cost = Costs.ExileFromGraveyard(1)
        effect = Effects.AddColorlessMana(1, restriction = ManaRestriction.ColoredSpellsOnly(withoutXInCost = true))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75d4e6f3-73cb-4799-bcb9-c1f5bc234a02.jpg?1783931016"
    }
}

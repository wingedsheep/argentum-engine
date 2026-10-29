package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Twinblade Assassins — Core Set 2021 #226 (canonical printing)
 * {3}{B}{G} · Creature — Elf Assassin · 5/4
 *
 * At the beginning of your end step, if a creature died this turn, draw a card.
 *
 * "If a creature died this turn" is an intervening-if (Osai Vultures' condition), checked as the
 * end step begins and again on resolution; it triggers once however many creatures died.
 */
val TwinbladeAssassins = card("Twinblade Assassins") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Elf Assassin"
    oracleText = "At the beginning of your end step, if a creature died this turn, draw a card."
    power = 5
    toughness = 4

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CreatureDiedThisTurn
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Campbell White"
        flavorText = "Rumors swirl of a pair of deadly Golgari assassins known only as the Left and Right Hands of the Ochran."
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5d387b19-a0a7-45c0-b1e9-71ca55fb4adc.jpg?1783930659"
        ruling("2020-06-23", "If a creature didn't die before your end step begins, the ability of Twinblade Assassins doesn't trigger at all. The creature may have died before Twinblade Assassins entered the battlefield, however.")
        ruling("2020-06-23", "The triggered ability triggers just once, no matter how many creatures died this turn.")
    }
}

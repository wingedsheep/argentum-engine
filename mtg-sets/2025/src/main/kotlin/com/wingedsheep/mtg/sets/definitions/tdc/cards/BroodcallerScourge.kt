package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Broodcaller Scourge — Tarkir: Dragonstorm Commander #44 (canonical printing)
 * {5}{G}{G} · Creature — Dragon · 5/7
 *
 * Flying
 * Whenever one or more Dragons you control deal combat damage to a player, you may put a
 * permanent card with mana value less than or equal to that damage from your hand onto the
 * battlefield.
 *
 * One trigger per player hit; "that damage" is the combat damage the Dragons dealt that player
 * in the step, bound as `triggerDamageAmount()`. `putFromHand`'s up-to-one choice is the "may".
 */
val BroodcallerScourge = card("Broodcaller Scourge") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 7
    oracleText = "Flying\n" +
        "Whenever one or more Dragons you control deal combat damage to a player, you may put a permanent card " +
        "with mana value less than or equal to that damage from your hand onto the battlefield."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.withSubtype("Dragon")).dealCombatDamageToAPlayer()
        effect = Patterns.Hand.putFromHand(
            GameObjectFilter.Permanent.manaValueAtMostDynamic(DynamicAmounts.triggerDamageAmount())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Loïc Canavaggia"
        flavorText = "Younger dragons bow to its will, not out of respect, but out of fear."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6fc0a74b-0b8a-4c70-87f2-f9f2625d5ff2.jpg?1783907167"
        ruling("2025-04-04", "If a card in your hand has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
    }
}

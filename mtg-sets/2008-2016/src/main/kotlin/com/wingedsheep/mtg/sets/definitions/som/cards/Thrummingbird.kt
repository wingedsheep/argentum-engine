package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Thrummingbird
 * {1}{U}
 * Creature — Phyrexian Bird Horror
 * 1/1
 *
 * Flying
 * Whenever this creature deals combat damage to a player, proliferate.
 */
val Thrummingbird = card("Thrummingbird") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Phyrexian Bird Horror"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Efrem Palacios"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc2dd336-e457-49a1-88ae-c35f0c846e99.jpg?1783941736"
    }
}

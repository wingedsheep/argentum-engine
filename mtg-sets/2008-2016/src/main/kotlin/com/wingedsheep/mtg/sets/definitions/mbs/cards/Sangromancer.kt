package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sangromancer
 * {2}{B}{B}
 * Creature — Vampire Shaman
 * 3/3
 * Flying
 * Whenever a creature an opponent controls dies, you may gain 3 life.
 * Whenever an opponent discards a card, you may gain 3 life.
 *
 * Both triggers are optional resolution-time yes/no gains (`optional = true`). The death trigger
 * fires once per opponent-controlled creature (Massacre Wurm's shape); the discard trigger fires
 * once per discarded card (Megrim's shape).
 *
 * Canonical printing lives in Mirrodin Besieged (the card's earliest real printing); later sets
 * contribute only [com.wingedsheep.sdk.model.Printing] rows.
 */
val Sangromancer = card("Sangromancer") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Shaman"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever a creature an opponent controls dies, you may gain 3 life.\n" +
        "Whenever an opponent discards a card, you may gain 3 life."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).dies()
        optional = true
        effect = Effects.GainLife(3)
        description = "Whenever a creature an opponent controls dies, you may gain 3 life."
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.discards()
        optional = true
        effect = Effects.GainLife(3)
        description = "Whenever an opponent discards a card, you may gain 3 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c374193-4ebb-4f33-a24d-e567aea57b01.jpg?1783941382"
        ruling(
            "2011-06-01",
            "If one or more creatures an opponent controls are put into the graveyard at the same " +
                "time as Sangromancer, Sangromancer's first ability will trigger that many times."
        )
    }
}

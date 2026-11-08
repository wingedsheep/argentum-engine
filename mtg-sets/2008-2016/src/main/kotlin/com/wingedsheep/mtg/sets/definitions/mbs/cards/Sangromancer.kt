package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sangromancer — Mirrodin Besieged #53 (canonical printing)
 * {2}{B}{B} · Creature — Vampire Shaman · 3/3
 *
 * Flying
 * Whenever a creature an opponent controls dies, you may gain 3 life.
 * Whenever an opponent discards a card, you may gain 3 life.
 *
 * Two independent "may" triggers: one per creature that dies, one per card discarded.
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
    }

    triggeredAbility {
        trigger = Triggers.anOpponent.discards()
        optional = true
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c374193-4ebb-4f33-a24d-e567aea57b01.jpg?1783941382"
    }
}

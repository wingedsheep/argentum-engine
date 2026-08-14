package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Alchemist's Greeting (Eldritch Moon #116)
 * {4}{R}
 * Sorcery
 *
 * Alchemist's Greeting deals 4 damage to target creature.
 * Madness {1}{R}
 *
 * Creature-only (not "any target"), so it can never go to the face. Madness (CR 702.35) turns the
 * five-mana sorcery into a two-mana instant-speed removal spell whenever something discards it.
 */
val AlchemistsGreeting = card("Alchemist's Greeting") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Alchemist's Greeting deals 4 damage to target creature.\n" +
        "Madness {1}{R} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)"

    spell {
        target = TargetCreature()
        effect = Effects.DealDamage(4, EffectTarget.ContextTarget(0))
    }

    madness("{1}{R}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Jakub Kasper"
        flavorText = "\"Impressive.\"\n—Chandra Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f33aaa1-cbaa-40a9-889e-3eca26b3a549.jpg?1783937468"
    }
}

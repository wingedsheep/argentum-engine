package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Immobilizer Eldrazi
 * {1}{R}
 * Creature — Eldrazi Drone
 * 2/1
 * Devoid (This card has no color.)
 * {2}{C}: Each creature with toughness greater than its power can't block this turn.
 * ({C} represents colorless mana.)
 *
 * The can't-block is a floating group restriction (`CantBlockGroup`), not a snapshot: it changes no
 * characteristics, so the affected set is re-evaluated against projected power and toughness when
 * blockers are declared (CR 611.2c) — exactly the card's ruling. It covers every creature, including
 * the activator's own.
 */
val ImmobilizerEldrazi = card("Immobilizer Eldrazi") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "{2}{C}: Each creature with toughness greater than its power can't block this turn. " +
        "({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Mana("{2}{C}")
        effect = Effects.CantBlockGroup(GroupFilter.AllCreatures.toughnessGreaterThanPower())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Svetlin Velinov"
        flavorText = "It denied the Zendikari the chance to stand and die fighting."
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95d9c92d-8b1b-4adc-8015-d50e673b6321.jpg?1783937910"
        ruling(
            "2016-01-22",
            "You compare the power and toughness as you declare blockers, not as Immobilizer " +
                "Eldrazi's ability resolves. For example, if you control a 3/3 creature as the ability " +
                "resolves, then later that turn it gets +0/+1 such that it's 3/4 as blockers are " +
                "declared, it won't be able to block."
        )
        ruling(
            "2016-01-22",
            "If a creature has already been legally declared as a blocker, Immobilizer Eldrazi's " +
                "ability won't change or undo that block."
        )
    }
}

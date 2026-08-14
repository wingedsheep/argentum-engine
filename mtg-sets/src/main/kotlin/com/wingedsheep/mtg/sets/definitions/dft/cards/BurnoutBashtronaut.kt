package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Burnout Bashtronaut
 * {R}
 * Creature — Goblin Warrior
 * 1/1
 * Menace
 * Start your engines! (If you have no speed, it starts at 1. It increases once on each of your turns when an opponent loses life. Max speed is 4.)
 * {2}: This creature gets +1/+0 until end of turn.
 * Max speed — This creature has double strike.
 */
val BurnoutBashtronaut = card("Burnout Bashtronaut") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    oracleText = "Menace\nStart your engines! (If you have no speed, it starts at 1. It increases " +
        "once on each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "{2}: This creature gets +1/+0 until end of turn.\n" +
        "Max speed — This creature has double strike."
    power = 1
    toughness = 1
    keywords(Keyword.MENACE)
    startYourEngines()
    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }
    maxSpeed { keywords(Keyword.DOUBLE_STRIKE) }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "115"
        artist = "Andrea Piparo"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4db66e7b-cb7a-4d86-a563-d570946aeb0d.jpg"
    }
}

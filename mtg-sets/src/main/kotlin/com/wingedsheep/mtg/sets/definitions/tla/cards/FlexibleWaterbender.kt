package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flexible Waterbender
 * {3}{U}
 * Creature — Human Warrior Ally
 * 2/5
 *
 * Vigilance
 * Waterbend {3}: This creature has base power and toughness 5/2 until end of turn.
 *
 * The waterbend cost is the existing activated-ability carrier (`hasWaterbend = true`, as on
 * Geyser Leaper); the body sets the creature's own base P/T via [Effects.SetBasePowerAndToughness]
 * (Layer 7b), defaulting to until end of turn.
 */
val FlexibleWaterbender = card("Flexible Waterbender") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Warrior Ally"
    power = 2
    toughness = 5
    oracleText = "Vigilance\n" +
        "Waterbend {3}: This creature has base power and toughness 5/2 until end of turn. " +
        "(While paying a waterbend cost, you can tap your artifacts and creatures to help. Each one pays for {1}.)"

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Mana("{3}")
        hasWaterbend = true
        effect = Effects.SetBasePowerAndToughness(5, 2, EffectTarget.Self)
        description = "This creature has base power and toughness 5/2 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Rafater"
        flavorText = "\"Water is the element of change.\"\n—Iroh"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/1447ebce-7e48-4b21-a39c-740920538bdd.jpg?1764120230"
    }
}

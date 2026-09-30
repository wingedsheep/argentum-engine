package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gulping Scraptrap
 * {4}{B}
 * Creature — Phyrexian Horror
 * 4/4
 *
 * When this creature enters or dies, proliferate.
 */
val GulpingScraptrap = card("Gulping Scraptrap") {
    manaCost = "{4}{B}"
    typeLine = "Creature — Phyrexian Horror"
    power = 4
    toughness = 4
    oracleText = "When this creature enters or dies, proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Proliferate()
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Mike Franchina"
        flavorText = "\"Does anyone else hear that horrible crunching sound?\"\n—Jace"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28f48394-c6aa-4453-954a-68195d9bd6ea.jpg?1783918045"
    }
}

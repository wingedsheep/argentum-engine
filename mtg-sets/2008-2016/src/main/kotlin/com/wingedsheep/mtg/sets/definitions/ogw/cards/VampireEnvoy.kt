package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Vampire Envoy
 * {2}{B}
 * Creature — Vampire Cleric Ally
 * 1/4
 * Flying
 * Whenever this creature becomes tapped, you gain 1 life.
 */
val VampireEnvoy = card("Vampire Envoy") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Cleric Ally"
    oracleText = "Flying\nWhenever this creature becomes tapped, you gain 1 life."
    power = 1
    toughness = 4
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.GainLife(1)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Johannes Voss"
        flavorText = "\"Zendikar will fight until it has bled its last.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3ba8e7aa-9a87-410e-b846-5f5c910585cf.jpg"
    }
}

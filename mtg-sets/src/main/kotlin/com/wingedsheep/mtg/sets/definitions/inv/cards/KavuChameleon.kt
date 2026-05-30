package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kavu Chameleon
 * {3}{G}{G}
 * Creature — Kavu
 * 4/4
 * This spell can't be countered.
 * {G}: This creature becomes the color of your choice until end of turn.
 */
val KavuChameleon = card("Kavu Chameleon") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Kavu"
    power = 4
    toughness = 4
    oracleText = "This spell can't be countered.\n" +
        "{G}: This creature becomes the color of your choice until end of turn."

    cantBeCountered = true

    activatedAbility {
        cost = Costs.Mana("{G}")
        effect = Effects.ChangeColorToChosen(EffectTarget.Self)
        description = "{G}: This creature becomes the color of your choice until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "John Howe"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f726437b-a41a-4ee9-b0ee-e09327508615.jpg?1562944770"
    }
}

package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity

/**
 * Starting Column — Aetherdrift #244
 * {3} · Artifact
 *
 * Start your engines!
 * {T}: Add one mana of any color.
 * Max speed — {T}, Sacrifice this artifact: Draw two cards, then discard a card.
 *
 * Two abilities that compete for the same tap: the mana ability is always available, and at max
 * speed the Column can instead cash itself in. The second is not a mana ability (it draws), so it
 * uses the stack and can be responded to, which is why only the first carries `manaAbility = true`.
 */
val StartingColumn = card("Starting Column") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "{T}: Add one mana of any color.\n" +
        "Max speed — {T}, Sacrifice this artifact: Draw two cards, then discard a card."

    startYourEngines()

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        description = "{T}: Add one mana of any color."
    }

    maxSpeed {
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
            effect = Patterns.Hand.loot(draw = 2, discard = 1)
            description = "{T}, Sacrifice this artifact: Draw two cards, then discard a card."
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "244"
        artist = "Jakub Kasper"
        flavorText = "Ten teams. Ten dreams. One winner."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0530b343-98c2-440a-b32e-d1566d318c3b.jpg?1783907846"
    }
}

package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Pendulum of Patterns
 * {2}
 * Artifact
 * When this artifact enters, you gain 3 life.
 * {5}, {T}, Sacrifice this artifact: Draw a card.
 *
 * An ETB life gain plus a three-atom activated cost — {5}, [Costs.Tap] and
 * [Costs.SacrificeSelf] — for a card.
 *
 * Aether Revolt is the earliest printing, so the canonical definition lives here; later sets
 * (M19 among them) carry `Printing` rows.
 */
val PendulumOfPatterns = card("Pendulum of Patterns") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "When this artifact enters, you gain 3 life.\n" +
        "{5}, {T}, Sacrifice this artifact: Draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3)
        description = "When this artifact enters, you gain 3 life."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
        description = "{5}, {T}, Sacrifice this artifact: Draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "192"
        artist = "Raoul Vitale"
        flavorText = "Its elaborate designs reveal secrets of aether's flow."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d19751aa-823e-4a0f-a004-dee333b34327.jpg"
        inBooster = false
    }
}

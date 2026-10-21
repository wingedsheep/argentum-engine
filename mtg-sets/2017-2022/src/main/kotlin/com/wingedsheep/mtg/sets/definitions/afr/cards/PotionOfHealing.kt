package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Potion of Healing
 * {1}{W}
 * Artifact
 * When this artifact enters, draw a card.
 * {W}, {T}, Sacrifice this artifact: You gain 3 life.
 */
val PotionOfHealing = card("Potion of Healing") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, draw a card.\n{W}, {T}, Sacrifice this artifact: You gain 3 life."
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.GainLife(3)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Pauline Voss"
        flavorText = "Magic can knit the body back together, but the courage to fight on comes only from within."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5edb608-c7d7-4fa9-b969-262c9789fc17.jpg?1783926524"
    }
}

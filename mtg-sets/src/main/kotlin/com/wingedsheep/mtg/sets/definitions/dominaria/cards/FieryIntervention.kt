package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fiery Intervention
 * {4}{R}
 * Sorcery
 * Choose one —
 * • Fiery Intervention deals 5 damage to target creature.
 * • Destroy target artifact.
 */
val FieryIntervention = card("Fiery Intervention") {
    manaCost = "{4}{R}"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n• Fiery Intervention deals 5 damage to target creature.\n• Destroy target artifact."

    spell {
        modal(chooseCount = 1) {
            mode("Fiery Intervention deals 5 damage to target creature.") {
                val t = target("target", Targets.Creature)
                effect = Effects.DealDamage(5, t)
            }
            mode("Destroy target artifact.") {
                val t = target("target", Targets.Artifact)
                effect = Effects.Destroy(t)
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "118"
        artist = "Adam Paquette"
        flavorText = "\"Burning something is easy. Choosing a target can be more difficult.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67f36a25-3692-4f2e-a25e-84b90656e6a4.jpg?1562737027"
    }
}

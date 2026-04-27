package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Pyrrhic Strike
 * {2}{W}
 * Instant
 *
 * As an additional cost to cast this spell, you may blight 2.
 * (You may put two -1/-1 counters on a creature you control.)
 * Choose one. If this spell's additional cost was paid, choose both instead.
 * • Destroy target artifact or enchantment.
 * • Destroy target creature with mana value 3 or greater.
 */
val PyrrhicStrike = card("Pyrrhic Strike") {
    manaCost = "{2}{W}"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, you may blight 2. " +
        "(You may put two -1/-1 counters on a creature you control.)\n" +
        "Choose one. If this spell's additional cost was paid, choose both instead.\n" +
        "• Destroy target artifact or enchantment.\n" +
        "• Destroy target creature with mana value 3 or greater."

    additionalCost(AdditionalCost.BlightOrPay(blightAmount = 2, alternativeManaCost = ""))

    spell {
        modal(chooseCount = 2, minChooseCount = 1, chooseAllIfBlightPaid = true) {
            mode("Destroy target artifact or enchantment") {
                val artifactOrEnchantment = target(
                    "artifact or enchantment",
                    TargetObject(filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment))
                )
                effect = Effects.Destroy(artifactOrEnchantment)
            }
            mode("Destroy target creature with mana value 3 or greater") {
                val creature = target(
                    "creature with mana value 3 or greater",
                    TargetObject(filter = TargetFilter(GameObjectFilter.Creature.manaValueAtLeast(3)))
                )
                effect = Effects.Destroy(creature)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cce5b16d-07fb-4e64-8ec9-b8b29ba86cff.jpg?1767732492"
    }
}

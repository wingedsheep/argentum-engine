package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wrath of the Skies
 * {X}{W}{W}
 * Sorcery
 * You get X {E} (energy counters), then you may pay any amount of {E}. Destroy each artifact,
 * creature, and enchantment with mana value less than or equal to the amount of {E} paid this way.
 *
 * Paying zero (or having no energy to pay) still destroys every mana-value-0 artifact, creature,
 * and enchantment (2024-06-07 ruling). The paid amount is stored in the pipeline and read back by
 * the destroy filter at resolution.
 */
val WrathOfTheSkies = card("Wrath of the Skies") {
    manaCost = "{X}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "You get X {E} (energy counters), then you may pay any amount of {E}. Destroy each " +
        "artifact, creature, and enchantment with mana value less than or equal to the amount of {E} " +
        "paid this way."

    spell {
        effect = Effects.AddDynamicCounters(CounterType.ENERGY, DynamicAmounts.xValue(), EffectTarget.Controller) then
            Effects.PayCounters(CounterType.ENERGY, storeAmountAs = "paid") then
            Effects.DestroyAll(
                GameObjectFilter.ArtifactCreatureOrEnchantment.manaValueAtMostDynamic(
                    DynamicAmounts.storedNumber("paid")
                )
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Franz Vohwinkel"
        flavorText = "Even the weather of Dominaria shifts to assail would-be invaders."
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ef1882e-b422-4f30-8a6c-bd71c2601660.jpg?1783911294"
        ruling("2024-06-07", "Choosing not to pay any {E} will have the same result as choosing to pay zero {E}. In either of these cases, each artifact, creature, and enchantment with a mana value of 0 will be destroyed.")
        ruling("2024-06-07", "If a permanent on the battlefield has {X} in its mana cost, X is 0 when determining its mana value.")
    }
}

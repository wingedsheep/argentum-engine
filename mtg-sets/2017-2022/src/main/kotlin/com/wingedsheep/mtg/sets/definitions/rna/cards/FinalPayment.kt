package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Final Payment
 * {W}{B}
 * Instant
 * As an additional cost to cast this spell, pay 5 life or sacrifice a creature or enchantment.
 * Destroy target creature.
 *
 * The cost-vs-cost additional cost is the general `Costs.additional.Choice` (Souls of the Lost's
 * shape): each payable option surfaces as its own cast action; the pay-life branch fills no
 * payment field, so the choice is recorded in a cast-choice slot (Bitter Triumph's shape).
 */
val FinalPayment = card("Final Payment") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, pay 5 life or sacrifice a creature or enchantment.\n" +
        "Destroy target creature."

    additionalCost(
        Costs.additional.Choice(
            Costs.additional.PayLife(5),
            Costs.additional.SacrificePermanent(GameObjectFilter.CreatureOrEnchantment),
            choiceSlot = ChoiceSlot.ADDITIONAL_COST_BRANCH,
        )
    )

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "171"
        artist = "Victor Adame Minguez"
        flavorText = "There's nothing quite like the feeling of paying off a large debt."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49a21a8f-9c7b-4ae8-8635-f2ee2151c8de.jpg?1783933651"
    }
}

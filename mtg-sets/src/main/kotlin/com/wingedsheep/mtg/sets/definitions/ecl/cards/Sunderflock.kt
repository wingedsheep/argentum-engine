package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Sunderflock
 * {7}{U}{U}
 * Creature — Elemental
 * 5/5
 * This spell costs {X} less to cast, where X is the greatest mana value among Elementals you control.
 * Flying
 * When this creature enters, if you cast it, return all non-Elemental creatures to their owners' hands.
 */
val Sunderflock = card("Sunderflock") {
    manaCost = "{7}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 5
    toughness = 5
    oracleText = "This spell costs {X} less to cast, where X is the greatest mana value among Elementals you control.\n" +
        "Flying\n" +
        "When this creature enters, if you cast it, return all non-Elemental creatures to their owners' hands."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.GreatestManaValueAmongPermanentsYouControl(
                    GameObjectFilter.Permanent.withSubtype(Subtype("Elemental")),
                ),
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasCast
        effect = GroupPatterns.returnAllToHand(
            GroupFilter(GameObjectFilter.Creature.notSubtype(Subtype("Elemental")))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5b6221e-cb22-45e4-bb98-2b960afc614c.jpg?1767732544"

        ruling("2025-11-17", "Once you announce you're casting a spell, no player may take actions until the spell has been paid for. Notably, opponents can't try to remove Elementals you control from the battlefield at that time.")
        ruling("2025-11-17", "To determine Sunderflock's total cost, start with the mana cost (or an alternative cost if another card's effect allows you to pay one instead), add any cost increases, then apply any cost reductions. Sunderflock's mana value remains unchanged, no matter what the total cost to cast it was.")
        ruling("2025-11-17", "If an Elemental on the battlefield has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
        ruling("2025-11-17", "Once you determine the cost to cast Sunderflock, you may activate mana abilities to pay that cost. If the greatest mana value among Elementals you control changes while activating mana abilities (probably because you sacrificed one or more Elementals), the cost to cast Sunderflock remains what you previously determined.")
    }
}

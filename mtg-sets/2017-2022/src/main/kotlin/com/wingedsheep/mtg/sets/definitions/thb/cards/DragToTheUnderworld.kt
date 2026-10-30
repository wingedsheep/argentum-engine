package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Drag to the Underworld
 * {2}{B}{B}
 * Instant
 * This spell costs {X} less to cast, where X is your devotion to black.
 * Destroy target creature.
 *
 * Deem Inferior's self-cast dynamic reduction fed [DynamicAmounts.devotionTo]: with no source
 * permanent, "you" is the caster, so it counts the {B} symbols among the caster's permanents at
 * cast time. Only generic mana is reduced (the {B}{B} is always paid), per the ruling.
 */
val DragToTheUnderworld = card("Drag to the Underworld") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "This spell costs {X} less to cast, where X is your devotion to black. (Each {B} in " +
        "the mana costs of permanents you control counts toward your devotion to black.)\n" +
        "Destroy target creature."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.devotionTo(Color.BLACK)),
            ),
        )
    }

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Victor Adame Minguez"
        flavorText = "The more you struggle against death, the tighter its grip becomes."
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91852444-9361-4588-a44f-fb90ba1b30e5.jpg?1783931570"
        ruling(
            "2020-01-24",
            "The cost reduction ability reduces only the generic mana in Drag to the Underworld's " +
                "cost. The colored mana must still be paid."
        )
        ruling(
            "2020-01-24",
            "To determine the total cost of a spell, start with the mana cost or alternative cost " +
                "you're paying, add any cost increases, then apply any cost reductions (such as that " +
                "of Drag to the Underworld). The mana value of the spell remains unchanged, no matter " +
                "what the total cost to cast it was."
        )
    }
}

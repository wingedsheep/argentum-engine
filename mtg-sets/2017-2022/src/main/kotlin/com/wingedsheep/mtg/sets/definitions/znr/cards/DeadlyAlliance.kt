package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Deadly Alliance — Zendikar Rising #96
 * {4}{B} · Instant
 *
 * This spell costs {1} less to cast for each creature in your party.
 * Destroy target creature or planeswalker.
 *
 * The discount is the same self-cast [ModifySpellCost] as Journey to Oblivion, reading
 * [DynamicAmounts.partySize] for the caster; it reduces only the generic part, so a full party
 * casts it for {B}.
 */
val DeadlyAlliance = card("Deadly Alliance") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast for each creature in your party. (Your party consists of " +
        "up to one each of Cleric, Rogue, Warrior, and Wizard.)\n" +
        "Destroy target creature or planeswalker."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.partySize()),
            ),
        )
    }

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Kieran Yanner"
        flavorText = "\"He could handle any one of us, but he's no match for all of us!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/0/007a5c8c-ed0b-4844-9393-a3d25d4ffa1d.jpg?1783929380"
        ruling(
            "2020-09-25",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're " +
                "paying, add any cost increases, then apply any cost reductions. The mana value of the spell " +
                "is determined only by its mana cost, no matter what the total cost to cast the spell was."
        )
    }
}

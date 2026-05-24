package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Gigastorm Titan
 * {4}{U}
 * Creature — Elemental
 * This spell costs {3} less to cast if you've cast another spell this turn.
 * 4/4
 *
 * The cost reduction is evaluated at cast time, before this spell's `CastSpellRecord`
 * is appended to the controller's spells-cast-this-turn history. So `atLeast = 1`
 * matches the oracle text "another spell" (i.e., at least one already-recorded cast
 * by this player).
 */
val GigastormTitan = card("Gigastorm Titan") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "This spell costs {3} less to cast if you've cast another spell this turn."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfCondition(
                    amount = 3,
                    condition = Conditions.YouCastSpellsThisTurn(atLeast = 1),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Bryan Sola"
        flavorText = "The shadow of the godcore falls over Uthros, and the world erupts with storms."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/abc83e0a-0ae5-4087-a751-058a1ba6a920.jpg?1752946778"
    }
}

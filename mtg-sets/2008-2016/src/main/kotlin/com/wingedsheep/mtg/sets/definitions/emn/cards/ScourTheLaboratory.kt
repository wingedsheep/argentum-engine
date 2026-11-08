package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Scour the Laboratory (Eldritch Moon #73)
 * {4}{U}{U}
 * Instant
 *
 * Delirium — This spell costs {2} less to cast if there are four or more card types among cards
 * in your graveyard.
 * Draw three cards.
 */
val ScourTheLaboratory = card("Scour the Laboratory") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Delirium — This spell costs {2} less to cast if there are four or more card types " +
        "among cards in your graveyard.\nDraw three cards."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.Delirium()),
        )
    }

    spell {
        effect = Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Greg Opalinski"
        flavorText = "\"The answer's in here somewhere!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c3247a4-f99a-4fff-a3a1-c3d51ed1a754.jpg"
    }
}

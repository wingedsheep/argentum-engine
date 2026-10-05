package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lookout's Dispersal
 * {2}{U}
 * Instant
 * This spell costs {1} less to cast if you control a Pirate.
 * Counter target spell unless its controller pays {4}.
 *
 * The discount is a [ModifySpellCost] static on the spell itself, gated on controlling a Pirate
 * (the Squash / Geistlight Snare shape); the counter is [Effects.CounterUnlessPays].
 */
val LookoutsDispersal = card("Lookout's Dispersal") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {1} less to cast if you control a Pirate.\n" +
        "Counter target spell unless its controller pays {4}."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(1),
            gating = CostGating.OnlyIf(
                Conditions.YouControl(GameObjectFilter.Permanent.withSubtype(Subtype.PIRATE))
            )
        )
    }

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{4}")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Ryan Yee"
        flavorText = "Her song plucks the strings of the storm, shifting wind and storm into a " +
            "harmony that will carry her ship to safety."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5751a3c-7695-4c47-9cbd-92fd5b1b7ec9.jpg?1783935780"

        ruling(
            "2017-09-29",
            "Once you announce that you're casting Lookout's Dispersal, no player may take other " +
                "actions until the spell's been paid for. Notably, players can't try to raise the " +
                "spell's cost by removing your Pirates."
        )
    }
}

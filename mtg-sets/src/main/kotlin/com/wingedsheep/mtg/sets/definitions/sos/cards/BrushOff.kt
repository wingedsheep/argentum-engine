package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Brush Off
 * {2}{U}{U}
 * Instant
 * This spell costs {1}{U} less to cast if it targets an instant or sorcery spell.
 * Counter target spell.
 *
 * The cost reduction is split into its generic and colored halves, each gated on the same
 * "targets an instant or sorcery spell" filter so they apply together:
 *  - {1} via [CostReductionSource.FixedIfAnyTargetMatches] on [CostModification.ReduceGenericBy].
 *  - {U} via [CostModification.ReduceColoredIfAnyTargetMatches].
 *
 * The reduction filter matches an instant or sorcery *spell on the stack* (the chosen target),
 * resolved at cast time once targets are announced (CR 601.2f).
 */
val BrushOff = card("Brush Off") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {1}{U} less to cast if it targets an instant or sorcery spell.\n" +
        "Counter target spell."

    spell {
        target = Targets.Spell
        effect = Effects.CounterSpell()
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(
                    amount = 1,
                    filter = GameObjectFilter.InstantOrSorcery,
                ),
            ),
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceColoredIfAnyTargetMatches(
                symbols = "{U}",
                filter = GameObjectFilter.InstantOrSorcery,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "39"
        artist = "Genel Jumalon"
        flavorText = "\"You're the one who brought a priceless scroll to a duel!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/5/151eab82-d20f-433b-b3bb-1d44e2871d5c.jpg?1775937183"
    }
}

package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Discontinuity
 * {3}{U}{U}{U}
 * Instant
 *
 * During your turn, this spell costs {2}{U}{U} less to cast.
 * End the turn.
 *
 * The {2}{U}{U} reduction is two self-cast cost modifications gated on it being your turn — the
 * generic {2} and the colored {U}{U} (Brush Off's split shape) — so on your turn it costs {1}{U}.
 * [Effects.EndTheTurn] then exiles the whole stack, this spell included, and skips to cleanup.
 */
val Discontinuity = card("Discontinuity") {
    manaCost = "{3}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "During your turn, this spell costs {2}{U}{U} less to cast.\n" +
        "End the turn. (Exile all spells and abilities from the stack, including this card. The player whose " +
        "turn it is discards down to their maximum hand size. Damage wears off, and \"this turn\" and " +
        "\"until end of turn\" effects end.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.IsYourTurn)
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceColored("{U}{U}"),
            gating = CostGating.OnlyIf(Conditions.IsYourTurn)
        )
    }

    spell {
        effect = Effects.EndTheTurn
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "48"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b33ba0a8-04e9-4df6-af20-a3ca4470cdcc.jpg?1783930730"
        ruling("2020-06-23", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying, add any cost increases, then apply any cost reductions (such as that of Discontinuity). The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
        ruling("2020-06-23", "Any \"at the beginning of the next end step\" triggered abilities won't get the chance to trigger that turn because the end step is skipped. Those abilities will trigger at the beginning of the end step of the next turn.")
    }
}

package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hurkyl's Final Meditation
 * {4}{U}{U}{U}
 * Instant
 * During turns other than yours, this spell costs {3} more to cast.
 * Return all nonland permanents to their owners' hands. End the turn.
 *
 * The tax is a self-cast [ModifySpellCost] gated on [Conditions.IsNotYourTurn], evaluated against
 * the caster as the spell is cast. The bounce resolves first, then [Effects.EndTheTurn] (CR 723.1)
 * exiles the rest of the stack — this spell included — and skips to the cleanup step.
 */
val HurkylsFinalMeditation = card("Hurkyl's Final Meditation") {
    manaCost = "{4}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "During turns other than yours, this spell costs {3} more to cast.\n" +
        "Return all nonland permanents to their owners' hands. End the turn. (Exile all spells and " +
        "abilities from the stack, including this card. The player whose turn it is discards down to " +
        "their maximum hand size. Damage wears off, and \"this turn\" and \"until end of turn\" effects end.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseGeneric(3),
            gating = CostGating.OnlyIf(Conditions.IsNotYourTurn)
        )
    }

    spell {
        effect = Patterns.Group.returnAllToHand(GroupFilter.AllNonlandPermanents) then
            Effects.EndTheTurn
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Nephelomancer"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5577f345-9577-42f8-b244-089b9a7c2e56.jpg?1783920110"
    }
}

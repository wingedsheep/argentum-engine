package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ertai's Scorn
 * {1}{U}{U}
 * Instant
 *
 * This spell costs {U} less to cast if an opponent cast two or more spells this turn.
 * Counter target spell.
 *
 * "An opponent cast two or more" is a per-opponent threshold — three opponents casting one spell
 * each don't add up — so the gate is [Conditions.AnOpponentCastSpellsThisTurnAtLeast] (the
 * greatest single opponent's count), not a summed count. The reduction is a flat {U} however many
 * opponents qualify (2022-09-09 ruling).
 */
val ErtaisScorn = card("Ertai's Scorn") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "This spell costs {U} less to cast if an opponent cast two or more spells this turn.\n" +
        "Counter target spell."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceColored("{U}"),
            gating = CostGating.OnlyIf(Conditions.AnOpponentCastSpellsThisTurnAtLeast(2))
        )
    }

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Alex Negrea"
        flavorText = "\"You're not so special, Karn. Anything that has been assembled can be disassembled.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6129b0d3-b50a-4a4b-9111-39f8fc6718dd.jpg?1783921354"
        ruling(
            "2022-09-09",
            "If multiple opponents have each cast two or more spells this turn, Ertai's Scorn costs only {U} less to cast, not {U} less for each of them."
        )
    }
}

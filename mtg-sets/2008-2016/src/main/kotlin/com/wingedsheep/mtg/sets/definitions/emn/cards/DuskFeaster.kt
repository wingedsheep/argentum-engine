package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Dusk Feaster (Eldritch Moon #88)
 * {5}{B}{B}
 * Creature — Vampire
 * 4/5
 *
 * Delirium — This spell costs {2} less to cast if there are four or more card types among cards
 * in your graveyard.
 * Flying
 */
val DuskFeaster = card("Dusk Feaster") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 4
    toughness = 5
    oracleText = "Delirium — This spell costs {2} less to cast if there are four or more card types " +
        "among cards in your graveyard.\nFlying"

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(2),
            gating = CostGating.OnlyIf(Conditions.Delirium()),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Anastasia Ovchinnikova"
        flavorText = "The Voldaren lands remained well protected when Olivia's army marched from " +
            "Lurenbraum Fortress."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d34a6fdc-5be9-4cb4-97cc-de63e8a0dde1.jpg"
    }
}

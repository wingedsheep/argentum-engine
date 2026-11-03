package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Siren Reaver
 * {3}{U}
 * Creature — Siren Pirate
 * 3/2
 *
 * Raid — This spell costs {1} less to cast if you attacked this turn.
 * Flying
 */
val SirenReaver = card("Siren Reaver") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Siren Pirate"
    power = 3
    toughness = 2
    oracleText = "Raid — This spell costs {1} less to cast if you attacked this turn.\nFlying"

    keywords(Keyword.FLYING)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(1),
            gating = CostGating.OnlyIf(Conditions.YouAttackedThisTurn)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Prepare to be boarded.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e5827a7-8ee3-4a89-b936-00a3283ddad3.jpg?1783935319"
    }
}

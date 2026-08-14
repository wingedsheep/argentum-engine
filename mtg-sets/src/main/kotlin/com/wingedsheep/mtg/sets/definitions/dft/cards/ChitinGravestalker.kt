package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Chitin Gravestalker — Aetherdrift #79
 * {5}{B} · Creature — Insect Warrior · 5/4
 *
 * This spell costs {1} less to cast for each artifact and/or creature card in your graveyard.
 * Cycling {2}
 *
 * Two existing primitives. The reduction is a self-cast [ModifySpellCost] over
 * [CostReductionSource.CardsInGraveyardMatchingFilter]; "artifact and/or creature card" is the
 * `Artifact or Creature` union, so an artifact creature card in the graveyard counts **once**
 * (the filter matches it, it isn't counted per type). Mana value is unaffected by the
 * reduction — Chitin Gravestalker is always mana value 6 (CR 202.3).
 */
val ChitinGravestalker = card("Chitin Gravestalker") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Insect Warrior"
    power = 5
    toughness = 4
    oracleText = "This spell costs {1} less to cast for each artifact and/or creature card in " +
        "your graveyard.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CardsInGraveyardMatchingFilter(
                    filter = GameObjectFilter.Artifact or GameObjectFilter.Creature
                )
            )
        )
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Slawomir Maniak"
        flavorText = "The Speedbrood doesn't differentiate between salvage and carrion."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/903b4141-04a3-44c4-9d3e-aa2a773d9883.jpg?1783907898"
    }
}

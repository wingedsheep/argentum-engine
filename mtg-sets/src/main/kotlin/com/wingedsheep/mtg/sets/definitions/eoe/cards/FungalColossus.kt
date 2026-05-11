package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Fungal Colossus
 * {6}{G}
 * Creature — Fungus Beast
 * This spell costs {X} less to cast, where X is the number of differently named lands you control.
 * 5/5
 */
val FungalColossus = card("Fungal Colossus") {
    manaCost = "{6}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Fungus Beast"
    oracleText = "This spell costs {X} less to cast, where X is the number of differently named lands you control."
    power = 5
    toughness = 5

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.DifferentlyNamedPermanentsYouControl(Filters.Land),
            ),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Sergey Glushakov"
        flavorText = "When falling shards of Kavaron shattered the icy shell of Evendo, the Eumidian seed pods were far from the only form of life awakened from slumber."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9dc9559f-cece-4c85-807c-158291666007.jpg?1752947307"
        ruling("2025-07-25", "To determine the number of differently named lands you control, count each land you control once, but only if its English name isn't exactly the same as another land you've already counted this way.")
        ruling("2025-07-25", "Fungal Colossus's mana value doesn't change no matter what the number of differently named lands you control is.")
        ruling("2025-07-25", "Once you determine the cost to cast Fungal Colossus, you may activate mana abilities to pay that cost. If the number of differently named lands you control changes while activating mana abilities (probably because you sacrificed one or more lands), the cost to cast Fungal Colossus remains what you previously determined.")
    }
}

package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Ravenous Cosmozoan
 * {5}{R}
 * Creature — Fish
 * 3/3
 *
 * This spell costs {1} less to cast for each noncreature, nonland card in your graveyard.
 * Flying, haste
 *
 * The cost reduction is a self-cast [ModifySpellCost] over
 * [CostReductionSource.CardsInGraveyardMatchingFilter]; it only reduces generic mana, so {R}
 * always remains.
 */
val RavenousCosmozoan = card("Ravenous Cosmozoan") {
    manaCost = "{5}{R}"
    typeLine = "Creature — Fish"
    power = 3
    toughness = 3
    oracleText = "This spell costs {1} less to cast for each noncreature, nonland card in your graveyard.\n" +
        "Flying, haste"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CardsInGraveyardMatchingFilter(
                    filter = GameObjectFilter.Noncreature and GameObjectFilter.Nonland,
                    amountPerCard = 1
                )
            )
        )
    }

    keywords(Keyword.FLYING, Keyword.HASTE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Bruce Brenneise"
        flavorText = "To a species evolved to sip solar winds and gnaw on asteroids, the rich plasma of " +
            "starship engines is like mother's milk."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8eeb471b-412d-4ea1-bec0-d7dd0a557d76.jpg?1793013221"
    }
}

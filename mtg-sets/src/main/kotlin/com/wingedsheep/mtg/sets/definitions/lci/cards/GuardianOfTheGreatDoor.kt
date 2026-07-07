package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Guardian of the Great Door
 * {W}{W}
 * Creature — Angel
 * 4/4
 * As an additional cost to cast this spell, tap four untapped artifacts, creatures, and/or
 * lands you control.
 * Flying
 *
 * The mandatory additional cost is modeled as AdditionalCost.TapPermanents over an inline
 * artifact-or-creature-or-land filter (CR 601.2f — paid as the spell is cast). The cost atom
 * enforces "untapped you control" independently of the filter, so no redundant state predicates
 * are added to the filter.
 */
val GuardianOfTheGreatDoor = card("Guardian of the Great Door") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    oracleText = "As an additional cost to cast this spell, tap four untapped artifacts, creatures, " +
        "and/or lands you control.\nFlying"
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    additionalCost(
        Costs.additional.TapPermanents(
            count = 4,
            filter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.IsArtifact,
                            CardPredicate.IsCreature,
                            CardPredicate.IsLand
                        )
                    )
                )
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a86f3cb2-7822-4c19-bd84-cea177e5b6e9.jpg?1782694599"
    }
}

package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Beyond the Quiet
 * {3}{W}{W}
 * Sorcery
 *
 * Exile all creatures and Spacecraft.
 */
val BeyondTheQuiet = card("Beyond the Quiet") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile all creatures and Spacecraft."

    spell {
        val creatureOrSpacecraft = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.HasSubtype(Subtype("Spacecraft"))))
            )
        )

        effect = Effects.ForEachInGroup(
            filter = GroupFilter(creatureOrSpacecraft),
            effect = Effects.Move(EffectTarget.Self, Zone.EXILE)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Yohann Schepacz"
        flavorText = "As the supervoid's photon collar washed over them, time stopped. Their souls would experience the grandeur of their demise for eternity."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce503869-8130-4afe-9691-4e90376b4bc4.jpg?1752946582"
    }
}

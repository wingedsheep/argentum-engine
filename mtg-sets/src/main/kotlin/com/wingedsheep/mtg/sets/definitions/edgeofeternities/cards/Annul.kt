package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetSpell
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Annul
 * {U}
 * Instant
 * Counter target artifact or enchantment spell.
 */
val Annul = card("Annul") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "Counter target artifact or enchantment spell."

    spell {
        target = TargetSpell(
            filter = TargetFilter(
                baseFilter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(
                            listOf(
                                CardPredicate.IsArtifact,
                                CardPredicate.IsEnchantment
                            )
                        )
                    )
                ),
                zone = Zone.STACK
            )
        )
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Carlos Palma Cruchaga"
        flavorText = "\"Your ship and crew have been contaminated by the great destroyers. We cannot risk their spread. Prepare for unraveling.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4feeebea-aa55-4599-ab5a-4e41a54d0dfd.jpg?1752946732"
    }
}

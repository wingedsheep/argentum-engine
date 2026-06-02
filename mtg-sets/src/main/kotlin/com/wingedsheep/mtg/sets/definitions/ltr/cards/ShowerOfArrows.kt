package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Shower of Arrows
 * {2}{G}
 * Instant
 *
 * Destroy target artifact, enchantment, or creature with flying. Scry 1.
 */
val ShowerOfArrows = card("Shower of Arrows") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target artifact, enchantment, or creature with flying. Scry 1."

    spell {
        // artifact, enchantment, or creature with flying
        val targetFilter = TargetFilter(
            GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.IsArtifact,
                            CardPredicate.IsEnchantment,
                            CardPredicate.And(
                                listOf(
                                    CardPredicate.IsCreature,
                                    CardPredicate.HasKeyword(Keyword.FLYING)
                                )
                            )
                        )
                    )
                )
            )
        )
        val permanent = target("target artifact, enchantment, or creature with flying", TargetObject(filter = targetFilter))
        effect = Effects.Destroy(permanent).then(LibraryPatterns.scry(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "188"
        artist = "Manuel Castañón"
        flavorText = "\"I wish there were more of your kin among us, Gimli. But even more would I give for a hundred good archers of Mirkwood. We shall need them.\"\n—Legolas"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92cd3884-18d1-4200-b28e-a52349ef37aa.jpg?1686969600"
    }
}

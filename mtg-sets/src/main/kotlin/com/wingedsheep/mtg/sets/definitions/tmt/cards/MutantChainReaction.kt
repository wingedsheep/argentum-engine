package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Mutant Chain Reaction
 * {2}{G}
 * Sorcery
 *
 * Destroy up to one target artifact, enchantment, or creature with flying.
 * Create a Mutagen token. (It's an artifact with "{1}, {T}, Sacrifice this
 * token: Put a +1/+1 counter on target creature. Activate only as a sorcery.")
 */
val MutantChainReaction = card("Mutant Chain Reaction") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy up to one target artifact, enchantment, or creature with flying. Create a Mutagen token. (It's an artifact with \"{1}, {T}, Sacrifice this token: Put a +1/+1 counter on target creature. Activate only as a sorcery.\")"

    spell {
        // "up to one target artifact, enchantment, or creature with flying"
        val permanent = target(
            "up to one target artifact, enchantment, or creature with flying",
            TargetPermanent(
                count = 1,
                optional = true,
                filter = TargetFilter(
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
            )
        )
        // The destroy no-ops if no target was chosen; the token is always created.
        effect = Effects.Composite(
            Effects.Destroy(permanent),
            Effects.CreateMutagenToken()
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Dominik Mayer"
        flavorText = "\"We live together, we train together, we fight together, we stand together . . . We are ninjas.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/7/8763e0ee-a48e-424f-8bce-132582d5944b.jpg?1771342410"
    }
}

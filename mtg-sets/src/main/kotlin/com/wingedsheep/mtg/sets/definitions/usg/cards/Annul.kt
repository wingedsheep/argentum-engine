package com.wingedsheep.mtg.sets.definitions.usg.cards

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
    colorIdentity = "U"
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
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Greg Simanson"
        flavorText = "The most effective way to destroy a spell is to ensure it was never cast in the first place."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f8c73ff-be92-41ca-93a7-76f9823adb38.jpg?1562908208"
    }
}

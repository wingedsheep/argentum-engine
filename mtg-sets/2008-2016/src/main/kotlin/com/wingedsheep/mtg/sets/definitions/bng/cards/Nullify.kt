package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Nullify
 * {U}{U}
 * Instant
 * Counter target creature or Aura spell.
 */
val Nullify = card("Nullify") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target creature or Aura spell."

    spell {
        target(
            TargetFilter(
                baseFilter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(
                            listOf(
                                CardPredicate.IsCreature,
                                CardPredicate.HasSubtype(Subtype.AURA)
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
        collectorNumber = "45"
        artist = "Adam Paquette"
        flavorText = "\"Those who incur Thassa's anger will be swallowed whole by her waves, or torn asunder by her bident.\"\n—Thrasios, triton hero"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a940d859-3fb1-4946-8277-b7c503605b1e.jpg?1783939566"
    }
}

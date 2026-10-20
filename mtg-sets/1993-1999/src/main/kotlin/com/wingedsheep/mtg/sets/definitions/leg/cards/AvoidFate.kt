package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Avoid Fate — Legends #175
 * {G} · Instant
 *
 * Counter target instant or Aura spell that targets a permanent you control.
 *
 * Same target shape as Ring of Immortals: a spell on the stack that is an instant or has the Aura
 * subtype, at least one of whose chosen targets is a permanent you control
 * ([CardPredicate.TargetsMatching]). An Aura spell targets the object it will enchant.
 */
val AvoidFate = card("Avoid Fate") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Counter target instant or Aura spell that targets a permanent you control."

    spell {
        target(
            TargetFilter(
                baseFilter = GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.Or(listOf(CardPredicate.IsInstant, CardPredicate.HasSubtype(Subtype.AURA))),
                        CardPredicate.TargetsMatching(GameObjectFilter.Permanent.youControl())
                    )
                ),
                zone = Zone.STACK
            )
        )
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "175"
        artist = "Phil Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92f1509e-6ed5-4009-a031-ea84b43cbd1b.jpg?1783948051"
    }
}

package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Ring of Immortals — Legends #293
 * {5} · Artifact
 *
 * {3}, {T}: Counter target instant or Aura spell that targets a permanent you control.
 *
 * The target is a spell on the stack that is an instant or has the Aura subtype and at least one of
 * whose chosen targets is a permanent you control ([CardPredicate.TargetsMatching], Teferi's
 * Response's shape). An Aura spell targets the object it will enchant, so its TargetsComponent
 * carries that permanent. The filter is rechecked on resolution, so a spell whose target has
 * changed away from your permanents makes the ability fizzle.
 */
val RingOfImmortals = card("Ring of Immortals") {
    manaCost = "{5}"
    typeLine = "Artifact"
    oracleText = "{3}, {T}: Counter target instant or Aura spell that targets a permanent you control."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
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
        rarity = Rarity.RARE
        collectorNumber = "293"
        artist = "Melissa A. Benson"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61706102-67fd-4167-bd7d-ec6da41db362.jpg?1783948026"
    }
}

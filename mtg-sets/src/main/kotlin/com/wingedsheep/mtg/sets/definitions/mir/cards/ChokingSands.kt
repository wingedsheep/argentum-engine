package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Choking Sands
 * {1}{B}{B}
 * Sorcery
 * Destroy target non-Swamp land. If that land was nonbasic, Choking Sands deals 2 damage
 * to the land's controller.
 */
val ChokingSands = card("Choking Sands") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target non-Swamp land. If that land was nonbasic, Choking Sands deals 2 damage to the land's controller."

    spell {
        target = TargetPermanent(
            filter = TargetFilter(
                GameObjectFilter.Land.notSubtype(Subtype.SWAMP)
            )
        )
        // Deal damage first (while the target is still on the battlefield with its
        // controller intact), then destroy. The conditional reads the target's current
        // nonbasic status, matching the past-tense oracle phrasing ("if that land was
        // nonbasic").
        effect = ConditionalEffect(
            condition = Conditions.TargetMatchesFilter(
                GameObjectFilter(
                    cardPredicates = listOf(
                        CardPredicate.IsLand,
                        CardPredicate.Not(CardPredicate.IsBasicLand),
                    )
                )
            ),
            effect = Effects.DealDamage(2, EffectTarget.TargetController)
        ) then Effects.Move(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Roger Raupp"
        flavorText = "\"The people wiped the sand from their eyes and cursed—and left the barren land to the hyenas and vipers.\"\n—Afari, Tales"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e41c15fb-01a1-446e-9e88-71e8e95d9bce.jpg?1562722377"
    }
}

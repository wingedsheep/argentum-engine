package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Molten Rain
 * {1}{R}{R}
 * Sorcery
 * Destroy target land. If that land was nonbasic, Molten Rain deals 2 damage to the land's controller.
 */
val MoltenRain = card("Molten Rain") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy target land. If that land was nonbasic, Molten Rain deals 2 damage to the land's controller."

    spell {
        target = Targets.Land
        // Same ordering as Choking Sands: deal the damage while the target is still on the
        // battlefield with its controller intact, then destroy. The conditional reads the
        // target's current nonbasic status, which matches the past-tense oracle phrasing
        // ("if that land was nonbasic").
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
        collectorNumber = "101"
        artist = "Hugh Jamieson"
        flavorText = "When the molten rains fall, entire landscapes melt and flow away in rivulets of fire."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f888b4d4-31f9-4322-8225-4d7e7a9f4dd5.jpg?1783944539"
        ruling(
            "2021-03-19",
            "If the target land is an illegal target by the time Molten Rain tries to resolve, the " +
                "spell doesn't resolve. No player is dealt 2 damage. If the target is legal but not " +
                "destroyed (most likely because it has indestructible), its controller is dealt 2 damage."
        )
    }
}

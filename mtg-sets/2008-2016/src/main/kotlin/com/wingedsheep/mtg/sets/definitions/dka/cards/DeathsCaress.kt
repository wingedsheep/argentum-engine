package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Death's Caress
 * {3}{B}{B}
 * Sorcery
 *
 * Destroy target creature. If that creature was a Human, you gain life equal to its toughness.
 *
 * Whether it was a Human and its toughness are both read as the creature last existed on the
 * battlefield, so the amount is stored before the destroy. Per the 2013 ruling the life is gained
 * even if the creature survives (indestructible, regeneration).
 */
val DeathsCaress = card("Death's Caress") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. If that creature was a Human, you gain life equal to its toughness."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            val life = storeNumber(
                DynamicAmounts.conditional(
                    Conditions.TargetMatchesFilter(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN), creature),
                    DynamicAmounts.toughnessOf(creature),
                    DynamicAmounts.fixed(0)
                )
            )
            run(Effects.Destroy(creature))
            run(Effects.GainLife(life.amount, EffectTarget.Controller))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/0/6/0643fb9a-8284-4dfc-836a-c2c69ef09f32.jpg?1783940831"
        ruling("2013-07-01", "If Death's Caress resolves but the target creature isn't destroyed, perhaps because it has indestructible or it regenerated, you still check if it's a Human. If so, you'll gain life equal to its toughness.")
        ruling("2011-01-22", "Check the creature as it last existed on the battlefield to determine if it was a Human. If it was, use its toughness as it last existed on the battlefield to determine how much life is gained.")
        ruling("2011-01-22", "If the target creature isn't on the battlefield or is otherwise an illegal target when Death's Caress tries to resolve, Death's Caress doesn't resolve and none of its effects will happen. You won't gain any life.")
    }
}

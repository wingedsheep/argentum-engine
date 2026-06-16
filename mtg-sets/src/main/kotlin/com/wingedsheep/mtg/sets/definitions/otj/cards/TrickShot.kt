package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.model.Rarity

/**
 * Trick Shot {4}{R}
 * Instant
 *
 * Trick Shot deals 6 damage to target creature and 2 damage to up to one other
 * target creature token.
 *
 * Two independent target requirements: a mandatory "target creature" (index 0) and
 * an optional "up to one other target creature token" (index 1). The second is
 * wrapped in [TargetOther] so it must differ from the first target, and uses an
 * `optional` [TargetObject] over the creature-token filter so it can be omitted.
 * The optional second [Effects.DealDamage] is a no-op when no second target is
 * chosen (the executor resolves an unset target to nothing).
 */
val TrickShot = card("Trick Shot") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Trick Shot deals 6 damage to target creature and 2 damage to up to one other target creature token."

    spell {
        target("creature", TargetObject(filter = TargetFilter.Creature))
        target(
            "tokenCreature",
            TargetOther(
                baseRequirement = TargetObject(
                    optional = true,
                    filter = TargetFilter(GameObjectFilter.Creature.token())
                )
            )
        )
        effect = Effects.Composite(
            listOf(
                Effects.DealDamage(6, EffectTarget.ContextTarget(0)),
                Effects.DealDamage(2, EffectTarget.ContextTarget(1))
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Brian Valeza"
        flavorText = "\"Cowerin' behind your buddy ain't gonna help, ya yellow-bellied lowlife.\"\n—Tyron, Slickshot duelist"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd3c2d02-67ca-4858-9b7a-3cfe8a08356c.jpg?1712355870"
    }
}

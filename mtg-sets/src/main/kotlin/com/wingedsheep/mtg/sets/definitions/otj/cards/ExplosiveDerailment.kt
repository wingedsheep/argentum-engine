package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Explosive Derailment {R}
 * Instant
 *
 * Spree (Choose one or more additional costs.)
 * + {2} — Explosive Derailment deals 4 damage to target creature.
 * + {2} — Destroy target artifact.
 *
 * Spree is modeled as a [ModalEffect] with `minChooseCount = 1`,
 * `chooseCount = modes.size`, and per-mode `additionalManaCost` (CR 702.166).
 * At least one mode must be chosen; the same mode can't be chosen more than once
 * (so `allowRepeat` is left at its default `false`). Each chosen mode targets
 * independently, so picking both modes lets you target a creature *and* an artifact.
 */
val ExplosiveDerailment = card("Explosive Derailment") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {2} — Explosive Derailment deals 4 damage to target creature.\n" +
        "+ {2} — Destroy target artifact."

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode(
                    effect = Effects.DealDamage(4, EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(Targets.Creature),
                    description = "+ {2} — Explosive Derailment deals 4 damage to target creature.",
                    additionalManaCost = "{2}"
                ),
                Mode(
                    effect = Effects.Destroy(EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(Targets.Artifact),
                    description = "+ {2} — Destroy target artifact.",
                    additionalManaCost = "{2}"
                )
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Leon Tukker"
        flavorText = "\"BOMBS AND TRAINS AND SABOTAGE!\"\n—Breeches"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0e3df9c-0a86-4e6f-a3c7-84a883328a3d.jpg?1712860585"

        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You choose the modes as you cast the spell with spree. Once modes are chosen, they can't be changed.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
        ruling("2024-04-12", "The mana value of a spell with spree is determined only by its mana cost. It doesn't matter which modes you choose or which additional costs you pay.")
    }
}

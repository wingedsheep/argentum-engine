package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Three Steps Ahead
 * {U}
 * Instant
 *
 * Spree (Choose one or more additional costs.)
 * + {1}{U} — Counter target spell.
 * + {3} — Create a token that's a copy of target artifact or creature you control.
 * + {2} — Draw two cards, then discard a card.
 *
 * Spree is modeled as a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`,
 * per-mode `additionalManaCost`, and `allowRepeat = false` (CR 700.2 / OTJ release notes). Modes
 * resolve in printed order. Modes that require a target (mode 1 counter, mode 2 copy) can only be
 * chosen when a legal target exists (CR 601.2c).
 */
val ThreeStepsAhead = card("Three Steps Ahead") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {1}{U} — Counter target spell.\n" +
        "+ {3} — Create a token that's a copy of target artifact or creature you control.\n" +
        "+ {2} — Draw two cards, then discard a card."

    spell {
        effect = ModalEffect(
            modes = listOf(
                // + {1}{U} — Counter target spell.
                Mode(
                    effect = Effects.CounterSpell(),
                    targetRequirements = listOf(Targets.Spell),
                    description = "+ {1}{U} — Counter target spell.",
                    additionalManaCost = "{1}{U}"
                ),
                // + {3} — Create a token that's a copy of target artifact or creature you control.
                Mode(
                    effect = Effects.CreateTokenCopyOfTarget(
                        target = EffectTarget.ContextTarget(0)
                    ),
                    targetRequirements = listOf(
                        TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrArtifact.youControl()))
                    ),
                    description = "+ {3} — Create a token that's a copy of target artifact or creature you control.",
                    additionalManaCost = "{3}"
                ),
                // + {2} — Draw two cards, then discard a card.
                Mode(
                    effect = Effects.DrawCards(2) then Effects.Discard(1),
                    description = "+ {2} — Draw two cards, then discard a card.",
                    additionalManaCost = "{2}"
                )
            ),
            chooseCount = 3,
            minChooseCount = 1,
            allowRepeat = false
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Francisco Miyara"
        flavorText = "She'd planned for every eventuality, with the obvious exception of failure."
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fffd839-2337-4a14-9312-cee085a17f4b.jpg?1712860611"
    }
}

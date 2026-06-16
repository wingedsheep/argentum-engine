package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Metamorphic Blast {U}
 * Instant
 *
 * Spree (Choose one or more additional costs.)
 * + {1} — Until end of turn, target creature becomes a white Rabbit with base power and toughness 0/1.
 * + {3} — Target player draws two cards.
 *
 * Spree (CR 702.166) is modeled as a [ModalEffect] with `minChooseCount = 1`,
 * `chooseCount = modes.size`, and per-mode `additionalManaCost`. At least one mode
 * must be chosen; the same mode can't be chosen more than once (`allowRepeat`
 * stays at its default `false`). Each chosen mode targets independently.
 *
 * Mode 1 uses [Effects.BecomeCreature] to set the target's base power/toughness to
 * 0/1 (Layer 7b SET_VALUES), its creature type to Rabbit (Layer 4), and its color
 * to white (Layer 5), all until end of turn.
 */
val MetamorphicBlast = card("Metamorphic Blast") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {1} — Until end of turn, target creature becomes a white Rabbit with base power and toughness 0/1.\n" +
        "+ {3} — Target player draws two cards."

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode(
                    effect = Effects.BecomeCreature(
                        target = EffectTarget.ContextTarget(0),
                        power = 0,
                        toughness = 1,
                        creatureTypes = setOf("Rabbit"),
                        colors = setOf(Color.WHITE.name),
                        duration = Duration.EndOfTurn
                    ),
                    targetRequirements = listOf(Targets.Creature),
                    description = "+ {1} — Until end of turn, target creature becomes a white Rabbit with base power and toughness 0/1.",
                    additionalManaCost = "{1}"
                ),
                Mode(
                    effect = Effects.DrawCards(2, EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(Targets.Player),
                    description = "+ {3} — Target player draws two cards.",
                    additionalManaCost = "{3}"
                )
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "57"
        artist = "Michal Ivan"
        flavorText = "Rundo's craving for revenge was replaced by a craving for vegetables."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd38d922-cfc1-43a8-82d8-5de441c71076.jpg?1712860596"

        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You choose the modes as you cast the spell with spree. Once modes are chosen, they can't be changed.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
    }
}

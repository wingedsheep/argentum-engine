package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rustler Rampage {W}
 * Instant
 *
 * Spree (Choose one or more additional costs.)
 * + {1} — Untap all creatures target player controls.
 * + {1} — Target creature gains double strike until end of turn.
 *
 * Spree is modeled as a [ModalEffect] with `minChooseCount = 1`,
 * `chooseCount = modes.size`, and per-mode `additionalManaCost` (CR 702.166).
 * Each chosen mode targets independently, so the player/creature target is
 * resolved per-mode via [EffectTarget.ContextTarget]. Mode 1 untaps every creature
 * the targeted player controls (`ForEachInGroup` over a target-relative group
 * filter); mode 2 grants double strike until end of turn.
 */
val RustlerRampage = card("Rustler Rampage") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {1} — Untap all creatures target player controls.\n" +
        "+ {1} — Target creature gains double strike until end of turn."

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode(
                    effect = Effects.ForEachInGroup(
                        filter = GroupFilter(
                            GameObjectFilter.Creature.targetPlayerControls(EffectTarget.ContextTarget(0))
                        ),
                        effect = Effects.Untap(EffectTarget.Self)
                    ),
                    targetRequirements = listOf(Targets.Player),
                    description = "+ {1} — Untap all creatures target player controls.",
                    additionalManaCost = "{1}"
                ),
                Mode(
                    effect = Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.ContextTarget(0)),
                    targetRequirements = listOf(Targets.Creature),
                    description = "+ {1} — Target creature gains double strike until end of turn.",
                    additionalManaCost = "{1}"
                )
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Josu Hernaiz"
        flavorText = "\"Release the cows!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33ed7ca3-894b-45f4-a15f-51b6bcd3f474.jpg?1712860605"

        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You choose the modes as you cast the spell with spree. Once modes are chosen, they can't be changed.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
    }
}

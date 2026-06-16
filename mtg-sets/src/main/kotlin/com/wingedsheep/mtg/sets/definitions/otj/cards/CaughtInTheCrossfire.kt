package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Caught in the Crossfire {R}{R}
 * Instant
 *
 * Spree (Choose one or more additional costs.)
 * + {1} — Caught in the Crossfire deals 2 damage to each outlaw creature.
 * + {1} — Caught in the Crossfire deals 2 damage to each non-outlaw creature.
 *
 * Spree is a [ModalEffect] with `minChooseCount = 1`, `chooseCount = modes.size`,
 * and per-mode `additionalManaCost` (CR 702.166). Choosing both modes hits every
 * creature (2 damage each). Each mode is a non-targeted [Effects.ForEachInGroup]
 * over the outlaw / non-outlaw creature group; the iterated creature is the
 * damage recipient ([EffectTarget.Self] inside the iteration body). Outlaws are
 * Assassins, Mercenaries, Pirates, Rogues, and Warlocks ([Filters.OutlawCreature]).
 */
val CaughtInTheCrossfire = card("Caught in the Crossfire") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Spree (Choose one or more additional costs.)\n" +
        "+ {1} — Caught in the Crossfire deals 2 damage to each outlaw creature. " +
        "(Assassins, Mercenaries, Pirates, Rogues, and Warlocks are outlaws.)\n" +
        "+ {1} — Caught in the Crossfire deals 2 damage to each non-outlaw creature."

    spell {
        effect = ModalEffect(
            modes = listOf(
                Mode(
                    effect = Effects.ForEachInGroup(
                        GroupFilter(Filters.OutlawCreature),
                        Effects.DealDamage(2, EffectTarget.Self)
                    ),
                    description = "+ {1} — Caught in the Crossfire deals 2 damage to each outlaw creature. (Assassins, Mercenaries, Pirates, Rogues, and Warlocks are outlaws.)",
                    additionalManaCost = "{1}"
                ),
                Mode(
                    effect = Effects.ForEachInGroup(
                        GroupFilter(Filters.NonOutlawCreature),
                        Effects.DealDamage(2, EffectTarget.Self)
                    ),
                    description = "+ {1} — Caught in the Crossfire deals 2 damage to each non-outlaw creature.",
                    additionalManaCost = "{1}"
                )
            ),
            chooseCount = 2,
            minChooseCount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "117"
        artist = "Xabi Gaztelua"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a2fd0c4-509e-49c2-ad57-f772efcbc207.jpg?1712860582"

        ruling("2024-04-12", "You must choose at least one of the listed modes and pay its associated additional cost in order to cast a spell with spree.")
        ruling("2024-04-12", "You choose the modes as you cast the spell with spree. Once modes are chosen, they can't be changed.")
        ruling("2024-04-12", "You can't choose the same mode more than once.")
        ruling("2024-04-12", "An outlaw is an Assassin, Mercenary, Pirate, Rogue, or Warlock. A creature is an outlaw if it has one or more of those creature types.")
    }
}

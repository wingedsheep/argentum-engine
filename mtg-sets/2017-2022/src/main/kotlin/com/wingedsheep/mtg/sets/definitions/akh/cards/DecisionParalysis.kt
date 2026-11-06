package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Decision Paralysis — Amonkhet #50
 * {3}{U} · Instant
 *
 * Tap up to two target creatures. Those creatures don't untap during their controller's next
 * untap step.
 *
 * Same shape as Frost Breath: one "up to two" requirement, each target tapped and given
 * DOESNT_UNTAP keyed to its own controller's next untap step.
 */
val DecisionParalysis = card("Decision Paralysis") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Tap up to two target creatures. Those creatures don't untap during their controller's next untap step."

    spell {
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Tap(EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                EffectTarget.ContextTarget(0),
                Duration.UntilAfterAffectedControllersNextUntap()
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Vincent Proce"
        flavorText = "\"Eyes see only what is possible. A trained mind can explore the impossible.\"\n—Kefnet, god of knowledge"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c8eb8d39-9eb8-4592-ab74-54cdc2460e62.jpg?1783936523"
    }
}

package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Frost Breath — Magic 2012 #54 (canonical printing)
 * {2}{U} · Instant
 *
 * Tap up to two target creatures. Those creatures don't untap during their controller's next
 * untap step.
 *
 * "Up to two target" is one requirement taking zero to two distinct creatures; each is tapped and
 * gets DOESNT_UNTAP keyed to its *own* controller's next untap step (the Tamiyo, Field Researcher
 * −2 pair). An already-tapped target just stays tapped and still skips that untap (ruling).
 */
val FrostBreath = card("Frost Breath") {
    manaCost = "{2}{U}"
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
        collectorNumber = "54"
        artist = "Mike Bierek"
        flavorText = "If you have to be stuck somewhere for a while, it's better to be stuck there with a friend."
        imageUri = "https://cards.scryfall.io/normal/front/1/7/1724ec5b-5437-4688-aa10-b327a0ae2654.jpg?1783941093"
        ruling("2020-06-23", "Frost Breath can target tapped creatures. If a targeted creature is already tapped when the spell resolves, that creature just remains tapped and doesn't untap during its controller's next untap step.")
    }
}

package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Selfless Police Captain
 * {1}{W}
 * Creature — Human Detective
 * 1/1
 * This creature enters with a +1/+1 counter on it.
 * When this creature leaves the battlefield, put its +1/+1 counters on target creature you control.
 *
 * "Enters with a counter" is a replacement effect (rule 614.1c), modeled with the shared
 * `EntersWithCounters(selfOnly = true)` replacement (cf. Servant of the Scale, Llanowar Elite).
 * The leaves-the-battlefield trigger reuses the `MoveAllLastKnownCounters` pattern (cf. Servant of
 * the Scale's dies trigger) — since the creature only ever bears +1/+1 counters, moving all
 * last-known counters is equivalent to the printed "its +1/+1 counters" wording. Unlike Servant's
 * dies trigger, this uses the broader `LeavesBattlefield` trigger, so it also fires on exile/bounce.
 */
val SelflessPoliceCaptain = card("Selfless Police Captain") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Detective"
    power = 1
    toughness = 1
    oracleText = "This creature enters with a +1/+1 counter on it.\n" +
        "When this creature leaves the battlefield, put its +1/+1 counters on target creature you control."

    replacementEffect(EntersWithCounters(
        counterType = CounterTypeFilter.PlusOnePlusOne,
        count = 1,
        selfOnly = true
    ))

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        target = Targets.CreatureYouControl
        effect = Effects.MoveAllLastKnownCounters(EffectTarget.ContextTarget(0))
        description = "When this creature leaves the battlefield, put its +1/+1 counters on target creature you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Aniekan Udofia"
        flavorText = "\"Peter always disappears when Spider-Man is around. How very odd.\"\n—Captain George Stacy"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb0fd7bd-10d0-4d29-af88-387d1e07f3b7.jpg?1783905362"
    }
}

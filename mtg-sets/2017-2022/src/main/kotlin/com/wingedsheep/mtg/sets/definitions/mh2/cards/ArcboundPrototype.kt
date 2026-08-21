package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Arcbound Prototype — Modern Horizons 2 #4
 * {1}{W} · Artifact Creature — Assembly-Worker · 0 / 0
 *
 * Modular 2 (This creature enters with two +1/+1 counters on it. When it dies, you may put its
 * +1/+1 counters on target artifact creature.)
 *
 * A vanilla body plus modular, so the whole card *is* the lowering. [KeywordAbility.modular] is
 * display-only vocabulary — nothing in the rules engine reads `Keyword.MODULAR` — so the two halves
 * the reminder text spells out are wired explicitly, exactly as `mh3/cards/ArcboundCondor.kt` does:
 *
 *  - the ETB half is an [EntersWithCounters] replacement (`selfOnly`, two +1/+1 counters). That is
 *    why the printed box is 0/0: a Prototype on the battlefield is a 2/2 made of counters, and it
 *    dies immediately as a state-based action if it ever loses them.
 *  - the death half is an *optional* dies trigger reading
 *    [ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT] rather than the live entity — the
 *    counters cease to exist the moment it changes zones, so the count must come from last-known
 *    information (CR 603.10 / 608.2h).
 *
 * That last point is also why this uses [Effects.AddDynamicCounters] with an explicit +1/+1 count
 * rather than the neighbouring `Effects.MoveAllLastKnownCounters` shape (Servant of the Scale):
 * moving *all* last-known counters would hand the target any leftover -1/-1 counters too, and
 * modular only ever moves the +1/+1 ones.
 */
val ArcboundPrototype = card("Arcbound Prototype") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Assembly-Worker"
    power = 0
    toughness = 0
    oracleText = "Modular 2 (This creature enters with two +1/+1 counters on it. When it dies, you may put its +1/+1 counters on target artifact creature.)"

    keywordAbility(KeywordAbility.modular(2))

    // Modular, half one: "This creature enters with two +1/+1 counters on it."
    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 2,
            selfOnly = true
        )
    )

    // Modular, half two: "When it dies, you may put its +1/+1 counters on target artifact creature."
    triggeredAbility {
        trigger = Triggers.Dies
        target = TargetPermanent(filter = TargetFilter(GameObjectFilter.ArtifactCreature))
        optional = true
        effect = Effects.AddDynamicCounters(
            Counters.PLUS_ONE_PLUS_ONE,
            DynamicAmount.ContextProperty(ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT),
            EffectTarget.ContextTarget(0)
        )
        description = "When this creature dies, you may put its +1/+1 counters on target artifact creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Svetlin Velinov"
        flavorText = "It flickered to life, saw itself alone, and began to build."
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1cb0a1a4-9216-47f8-a4e3-20fe0de6a518.jpg?1783926897"
    }
}

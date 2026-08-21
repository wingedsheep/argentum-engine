package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
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
 * Arcbound Mouser — Modern Horizons 2 #3
 * {W} · Artifact Creature — Cat · 0 / 0
 *
 * Lifelink
 * Modular 1 (This creature enters with a +1/+1 counter on it. When it dies, you may put its +1/+1
 * counters on target artifact creature.)
 *
 * **Modular is lowered here, not handled by the engine.** [KeywordAbility.modular] is display-only
 * vocabulary — nothing in the rules engine reads `Keyword.MODULAR` — so the two halves the reminder
 * text spells out are wired explicitly, exactly as `mh3/cards/ArcboundCondor.kt` does:
 *
 *  - the ETB half is an [EntersWithCounters] replacement (`selfOnly`, one +1/+1 counter). That is
 *    why the printed box is 0/0: a Mouser on the battlefield is a 1/1 made entirely of counters.
 *  - the death half is an *optional* dies trigger reading
 *    [ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT] rather than the live entity — the
 *    counters cease to exist the moment the Mouser changes zones, so the count must come from
 *    last-known information (CR 603.10 / 608.2h).
 *
 * That last point is also why this uses [Effects.AddDynamicCounters] with an explicit +1/+1 count
 * rather than the neighbouring `Effects.MoveAllLastKnownCounters` shape (Servant of the Scale):
 * moving *all* last-known counters would hand the target any leftover -1/-1 counters too, and
 * modular only ever moves the +1/+1 ones.
 */
val ArcboundMouser = card("Arcbound Mouser") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Cat"
    power = 0
    toughness = 0
    oracleText = "Lifelink\n" +
        "Modular 1 (This creature enters with a +1/+1 counter on it. When it dies, you may put its +1/+1 counters on target artifact creature.)"

    keywords(Keyword.LIFELINK)
    keywordAbility(KeywordAbility.modular(1))

    // Modular, half one: "This creature enters with a +1/+1 counter on it."
    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 1,
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
        collectorNumber = "3"
        artist = "Campbell White"
        flavorText = "It doesn't purr. It hums."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d66e724-49ee-4f08-a160-584350de1d95.jpg?1783926896"
    }
}

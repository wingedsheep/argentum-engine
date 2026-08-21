package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.riot
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
 * Arcbound Slasher — Modern Horizons 2 #111
 * {4}{R} · Artifact Creature — Cat · 0 / 0
 *
 * Modular 4 (This creature enters with four +1/+1 counters on it. When it dies, you may put its
 * +1/+1 counters on target artifact creature.)
 * Riot (This creature enters with your choice of an additional +1/+1 counter or haste.)
 *
 * Two entry-time mechanics stack here, and they are lowered differently because the SDK supports
 * them differently.
 *
 * **Riot** has a helper: [riot] wires the whole Khans-Siege `EntersWithChoice(MODE)` pattern — the
 * mode choice, the mode-gated +1/+1 counter, and the mode-gated haste grant. Nothing extra belongs
 * next to it; adding a second [EntersWithCounters] for riot would double the counter.
 *
 * **Modular** has none: [KeywordAbility.modular] is display-only vocabulary — nothing in the rules
 * engine reads `Keyword.MODULAR` — so the two halves the reminder text spells out are wired
 * explicitly, exactly as `mh3/cards/ArcboundCondor.kt` does:
 *
 *  - the ETB half is its *own* [EntersWithCounters] replacement (`selfOnly`, four +1/+1 counters),
 *    independent of riot's. Both replacement effects apply as the Slasher enters (CR 616.1), which
 *    is exactly what "an **additional** +1/+1 counter" means: choose the counter mode and it enters
 *    a 5/5, choose haste and it enters a hasty 4/4. The printed box is 0/0 because the body is made
 *    entirely of those counters.
 *  - the death half is an *optional* dies trigger reading
 *    [ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT] rather than the live entity — the
 *    counters cease to exist the moment it changes zones, so the count must come from last-known
 *    information (CR 603.10 / 608.2h). Reading that key rather than the neighbouring
 *    `Effects.MoveAllLastKnownCounters` shape also keeps riot's counter in scope (it is a plain
 *    +1/+1 counter) while leaving any -1/-1 counters behind, which is what modular moves.
 */
val ArcboundSlasher = card("Arcbound Slasher") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Cat"
    power = 0
    toughness = 0
    oracleText = "Modular 4 (This creature enters with four +1/+1 counters on it. When it dies, you may put its +1/+1 counters on target artifact creature.)\n" +
        "Riot (This creature enters with your choice of an additional +1/+1 counter or haste.)"

    keywordAbility(KeywordAbility.modular(4))

    // Modular, half one: "This creature enters with four +1/+1 counters on it."
    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 4,
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

    // Riot (printed) — the helper lowers the choice, the counter and the haste grant.
    riot()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dcafff1a-e220-40ff-8c00-de6037219bc6.jpg?1783926851"
    }
}

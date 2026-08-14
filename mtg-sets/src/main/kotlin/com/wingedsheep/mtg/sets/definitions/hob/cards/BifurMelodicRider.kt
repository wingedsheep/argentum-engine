package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.TriggeredAbilityBuilder
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.storied
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bifur, Melodic Rider
 * {4}{R/W}{R/W}
 * Legendary Creature — Dwarf Bard
 * 4/5
 *
 * Storied.
 * Whenever Bifur enters or attacks, put a +1/+1 counter on target creature.
 * As long as you have an enduring story, if a triggered ability of a Dwarf you control triggers,
 * that ability triggers an additional time.
 *
 * The doubler is [AdditionalSourceTriggers] — the generic "if a triggered ability of a permanent
 * matching the filter you control triggers, it triggers an additional time" static. Two parameter
 * choices carry the oracle text:
 *  - `excludeSelf = false`, because the text says "a Dwarf you control", not "*another* Dwarf".
 *    Bifur is himself a Dwarf you control, so his own enters/attacks trigger is doubled — the
 *    printed ruling spells this out ("If Bifur entering causes you to have an enduring story, his
 *    'enters or attacks' ability triggers an additional time").
 *  - `condition = Conditions.YouHaveEnduringStory` rather than a
 *    [com.wingedsheep.sdk.scripting.ConditionalStaticAbility] wrapper:
 *    `TriggerDetector.duplicateSourceTriggers` scans battlefield statics for a bare
 *    `AdditionalSourceTriggers`, so a wrapped one would never be seen. The field exists for exactly
 *    this shape and is evaluated against the doubler's source and controller.
 *
 * Because the doubler causes a *second trigger* rather than copying the first, each instance
 * chooses its own target independently (CR 603.2d) — which is what the engine's duplicate-then-
 * target flow already does.
 *
 * "Enters or attacks" is the repo's two-ability idiom (Queen's Bay Paladin, Sentinel of the
 * Nameless City): one `EntersBattlefield` and one `Attacks` ability sharing a rider.
 */
val BifurMelodicRider = card("Bifur, Melodic Rider") {
    manaCost = "{4}{R/W}{R/W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Dwarf Bard"
    oracleText = "Storied (If you control three or more artifacts, legendaries, and/or Sagas, you " +
        "have an enduring story for the rest of the game.)\n" +
        "Whenever Bifur enters or attacks, put a +1/+1 counter on target creature.\n" +
        "As long as you have an enduring story, if a triggered ability of a Dwarf you control " +
        "triggers, that ability triggers an additional time."
    power = 4
    toughness = 5

    storied()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        counterRider()
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        counterRider()
    }

    staticAbility {
        ability = AdditionalSourceTriggers(
            sourceFilter = GameObjectFilter.Creature.withSubtype(Subtype.DWARF).youControl(),
            excludeSelf = false,
            condition = Conditions.YouHaveEnduringStory,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee88cc80-8fbf-451c-b2b8-09158426c26a.jpg?1784377019"
    }
}

/** The rider shared by the enters and attacks halves: a +1/+1 counter on one target creature. */
private fun TriggeredAbilityBuilder.counterRider() {
    val creature = target("target creature", Targets.Creature)
    effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
}

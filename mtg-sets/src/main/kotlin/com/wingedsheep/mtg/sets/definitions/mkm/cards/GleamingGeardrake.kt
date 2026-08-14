package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gleaming Geardrake — Murders at Karlov Manor #205
 * {U}{R} · Artifact Creature — Drake · 1/1
 *
 * Flying
 * When this creature enters, investigate.
 * Whenever you sacrifice an artifact, put a +1/+1 counter on this creature.
 *
 * The two abilities are a closed loop by design: the enters trigger hands you the Clue, and
 * cracking that Clue is a sacrifice the second ability sees. Nothing about that needs wiring — a
 * Clue's own "{2}, Sacrifice this token: Draw a card" routes through the same sacrifice hook as any
 * other, so [Triggers.YouSacrificeA] over `GameObjectFilter.Artifact` picks it up.
 *
 * `YouSacrificeA` (per-permanent, bare article) rather than `YouSacrificeOneOrMore` (batched): the
 * text says "an artifact", so sacrificing three artifacts to one cost puts three counters on the
 * Drake, not one (CR 603.2c). And `YouSacrificeA` rather than `YouSacrificeAnother` — the Geardrake
 * is itself an artifact and the oracle text has no "another", so it counts itself. That last case
 * is not idle: the trigger still goes on the stack when the Drake is the artifact sacrificed, and
 * then resolves with nothing to put a counter on.
 */
val GleamingGeardrake = card("Gleaming Geardrake") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Artifact Creature — Drake"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "When this creature enters, investigate. (Create a Clue token. It's an artifact with " +
        "\"{2}, Sacrifice this token: Draw a card.\")\n" +
        "Whenever you sacrifice an artifact, put a +1/+1 counter on this creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Investigate()
        description = "When this creature enters, investigate."
    }

    triggeredAbility {
        trigger = Triggers.YouSacrificeA(GameObjectFilter.Artifact)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you sacrifice an artifact, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Filipe Pagliuso"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/cabb5875-42ff-4e3a-a32e-aab392fccff8.jpg?1783912848"
    }
}

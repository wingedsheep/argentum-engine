package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tireless Tracker
 * {2}{G}
 * Creature — Human Scout
 * 3/2
 *
 * Landfall — Whenever a land you control enters, investigate.
 * Whenever you sacrifice a Clue, put a +1/+1 counter on this creature.
 *
 * Both halves are existing vocabulary:
 * - The landfall half is [Triggers.LandYouControlEnters] + [Effects.Investigate] (CR 701.36 — the
 *   keyword-action spelling of "create a Clue token").
 * - "Whenever you sacrifice **a** Clue" is the per-permanent template ([Triggers.YouSacrificeA],
 *   CR 603.2c), not the batch one: sacrificing two Clues at once yields two +1/+1 counters. Per the
 *   2024-02-02 ruling a Clue is any Clue *artifact*, not just a token, so the filter keys on the
 *   artifact subtype rather than on token-ness — and it fires for a Clue sacrificed for any reason,
 *   not only to its own draw ability.
 */
val TirelessTracker = card("Tireless Tracker") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Scout"
    power = 3
    toughness = 2
    oracleText = "Landfall — Whenever a land you control enters, investigate. (Create a Clue token. " +
        "It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")\n" +
        "Whenever you sacrifice a Clue, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.Investigate()
        description = "Landfall — Whenever a land you control enters, investigate."
    }

    triggeredAbility {
        trigger = Triggers.YouSacrificeA(GameObjectFilter.Artifact.withSubtype("Clue"))
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you sacrifice a Clue, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee8e9928-d9b2-4570-adb8-44b34115decd.jpg?1783937719"

        ruling(
            "2024-11-08",
            "A landfall ability triggers whenever a land you control enters for any reason. It " +
                "triggers whenever you play a land, as well as whenever a spell or ability puts a " +
                "land onto the battlefield under your control."
        )
        ruling(
            "2024-11-08",
            "A landfall ability doesn't trigger if a permanent already on the battlefield becomes a land."
        )
        ruling(
            "2024-02-02",
            "Some abilities trigger \"whenever you sacrifice a Clue\". Those abilities trigger " +
                "whenever you sacrifice a Clue for any reason, not just to activate a Clue's " +
                "activated ability."
        )
        ruling(
            "2024-02-02",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact token."
        )
    }
}

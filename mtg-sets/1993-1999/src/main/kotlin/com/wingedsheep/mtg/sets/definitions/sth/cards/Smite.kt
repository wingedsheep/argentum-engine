package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Smite
 * {W}
 * Instant
 * Destroy target blocked creature.
 *
 * Canonical printing: Stronghold (1998) is Smite's earliest real-expansion printing, so the
 * [com.wingedsheep.sdk.model.CardDefinition] lives here. Later printings (Rise of the Eldrazi,
 * Gatecrash, Tempest Remastered) contribute only a `Printing(...)` row.
 *
 * "Blocked creature" is the durable combat status of CR 509.1h, not a live "someone is blocking
 * it right now" check — [Targets.BlockedCreature] stays satisfied through the end of combat even
 * after every blocker has died or left combat.
 */
val Smite = card("Smite") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target blocked creature."

    spell {
        val victim = target("target", Targets.BlockedCreature)
        effect = Effects.Destroy(victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Daren Bader"
        flavorText = "\"You've got your childhood wish at last. Now you get to die.\"\n—Gerrard, to Volrath"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14f165ad-cfe6-4a5d-8073-a70969494855.jpg?1783946572"
        ruling(
            "2010-06-15",
            "A \"blocked creature\" is an attacking creature that has been blocked by a creature this " +
                "combat, or has become blocked as the result of a spell or ability this combat. Unless the " +
                "attacking creature leaves combat, it continues to be a blocked creature through the end of " +
                "combat step, even if the creature or creatures that blocked it are no longer on the " +
                "battlefield or have otherwise left combat by then."
        )
    }
}

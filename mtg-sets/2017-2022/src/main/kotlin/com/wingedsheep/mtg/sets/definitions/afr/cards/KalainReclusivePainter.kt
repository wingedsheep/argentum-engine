package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kalain, Reclusive Painter {B}{R}
 * Legendary Creature — Human Elf Bard 1/2
 *
 * When Kalain enters, create a Treasure token.
 * Other creatures you control enter with an additional +1/+1 counter on them for each mana from a
 * Treasure spent to cast them.
 *
 * The second ability is an `EntersWithDynamicCounters(otherOnly = true)`. The engine evaluates an
 * other-only enters count against the ENTERING creature, so `manaSpentFromSubtype(TREASURE)` reads
 * that creature's own recorded cast payment — a creature put onto the battlefield without being
 * cast spent no mana and gets no counters.
 */
val KalainReclusivePainter = card("Kalain, Reclusive Painter") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Elf Bard"
    power = 1
    toughness = 2
    oracleText = "When Kalain enters, create a Treasure token. (It's an artifact with \"{T}, Sacrifice " +
        "this token: Add one mana of any color.\")\n" +
        "Other creatures you control enter with an additional +1/+1 counter on them for each mana " +
        "from a Treasure spent to cast them."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateTreasure()
    }

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmounts.manaSpentFromSubtype(Subtype.TREASURE),
            otherOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "225"
        artist = "Justine Cruz"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e45e9552-7737-4c61-89b0-5a3bec3c8f34.jpg?1783926447"
    }
}

package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleCounterPlacement
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter

/**
 * Loading Zone
 * {3}{G}
 * Enchantment
 *
 * If one or more counters would be put on a creature, Spacecraft, or Planet you control,
 * twice that many of each of those kinds of counters are put on it instead.
 * Warp {G}
 *
 * Functions like a Doubling Season restricted to creature/Spacecraft/Planet permanents
 * you control, for any counter kind. Recipient filter is the "you control" gate, so
 * placedByYou = false (effect applies regardless of who is placing the counters).
 */
val LoadingZone = card("Loading Zone") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "If one or more counters would be put on a creature, Spacecraft, or Planet you control, twice that many of each of those kinds of counters are put on it instead.\n" +
        "Warp {G} (You may cast this card from your hand for its warp cost. Exile this enchantment at the beginning of the next end step, then you may cast it from exile on a later turn.)"

    replacementEffect(
        DoubleCounterPlacement(
            placedByYou = false,
            appliesTo = GameEvent.CounterPlacementEvent(
                counterType = CounterTypeFilter.Any,
                recipient = RecipientFilter.Matching(
                    GameObjectFilter.Creature.youControl()
                        .or(GameObjectFilter.Permanent.withSubtype("Spacecraft").youControl())
                        .or(GameObjectFilter.Permanent.withSubtype("Planet").youControl())
                )
            )
        )
    )

    warp = "{G}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "196"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d2c95bd-79af-4a23-b265-62cc0b164e3e.jpg?1752947354"
        ruling("2025-07-25", "If a creature, Spacecraft, or Planet you control would enter the battlefield with a number of counters on it, it enters with twice that many instead.")
        ruling("2025-07-25", "If you control two Loading Zones, the number of counters put on a creature, Spacecraft, or Planet you control is four times the original number. Three Loading Zones multiplies the original number by eight, and so on.")
        ruling("2025-07-25", "If two or more effects attempt to modify how many counters would be put onto a permanent you control, you choose the order to apply those effects, no matter who controls the sources of those effects.")
    }
}

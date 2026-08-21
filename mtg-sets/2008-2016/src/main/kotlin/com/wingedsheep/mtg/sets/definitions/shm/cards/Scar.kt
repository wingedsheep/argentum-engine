package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Scar
 * {B/R}
 * Instant
 *
 * Put a -1/-1 counter on target creature.
 *
 * - [Counters.MINUS_ONE_MINUS_ONE] is the canonical `-1/-1` string constant; spelling the
 *   counter type by hand is the classic way to end up with a counter the engine treats as a
 *   bespoke named counter instead of the real one.
 * - The counter is a permanent stat change, not a until-end-of-turn pump, so no duration is
 *   involved — and a creature reduced to 0 toughness dies to state-based actions.
 */
val Scar = card("Scar") {
    manaCost = "{B/R}"
    typeLine = "Instant"
    oracleText = "Put a -1/-1 counter on target creature."

    spell {
        val t = target("target", Targets.Creature)
        effect = Effects.AddCounters(Counters.MINUS_ONE_MINUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "Pete Venters"
        flavorText = "\"What is a scar? A sign of courage in the face of danger? A mere trick of flesh? Or a constant reminder of agonizing pain, and the follies of war.\"\n" +
            "—Illulia of Nighthearth"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b34e3f7c-468a-456c-8ed0-0cb88f6d86fc.jpg?1783942724"
    }
}

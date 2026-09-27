package com.wingedsheep.mtg.sets.definitions.tle.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Heartbeat of Spring reprint in TLE.
 * Canonical CardDefinition lives in Champions of Kamigawa (its earliest real printing),
 * `com.wingedsheep.mtg.sets.definitions.chk.cards.HeartbeatOfSpring`.
 */
val HeartbeatOfSpringTleReprint = Printing(
    oracleId = "f15911a5-f597-452e-a015-b115339f28fc",
    name = "Heartbeat of Spring",
    setCode = "TLE",
    collectorNumber = "42",
    scryfallId = "006b8547-1427-4aa2-88e4-e82b0ff4bb6d",
    artist = "Viacom",
    imageUri = "https://cards.scryfall.io/normal/front/0/0/006b8547-1427-4aa2-88e4-e82b0ff4bb6d.jpg?1783904848",
    releaseDate = "2025-11-21",
    rarity = Rarity.MYTHIC,
)

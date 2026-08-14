package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

/**
 * The game's day/night designation (CR 731, "Day and Night").
 *
 * Day and night are designations the *game itself* can have — there is exactly one per game, not
 * one per player or permanent. Per CR 731.1 the game starts with **neither** designation (modeled
 * as a `null` value wherever this is stored), and once it has become day or night it always has
 * exactly one of the two from that point forward.
 *
 * The designation changes through:
 *  - the untap-step turn-based action (CR 502.2 / 731.2) — becomes night if it's day and the
 *    previous turn's active player cast no spells; becomes day if it's night and they cast two or
 *    more;
 *  - the daybound/nightbound keyword abilities (CR 702.145d/g) — controlling such a permanent while
 *    it's neither day nor night makes it day (daybound) or night (nightbound);
 *  - other effects that say "it becomes day" / "it becomes night" (e.g. Into the Night).
 *
 * All writes go through the engine's `DayNightService`, mirroring how `SpeedService` is the single
 * writer for [Speed].
 */
@Serializable
enum class DayNight(val displayName: String) {
    DAY("Day"),
    NIGHT("Night");

    /** The opposite designation — CR 731.1a "day becomes night" / "night becomes day". */
    fun opposite(): DayNight = when (this) {
        DAY -> NIGHT
        NIGHT -> DAY
    }
}

package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword

/**
 * Add **Daybound** (CR 702.145, Innistrad: Midnight Hunt / Crimson Vow) to a transforming
 * double-faced card's **front** face.
 *
 * Like [startYourEngines], the whole design is "just the keyword" — daybound's three static
 * abilities are load-bearing behavior the engine reads off projected state, not per-card wiring:
 *
 *  - **"If it is night … it enters transformed"** — `StackResolver` checks for [Keyword.DAYBOUND] and
 *    the game's night designation when the DFC resolves onto the battlefield (CR 702.145b, first
 *    ability), entering it back-face-up without emitting a transform.
 *  - **"As it becomes night, if front face up, transform it"** — the
 *    `com.wingedsheep.engine.mechanics.daynight.DayNightService` transform cascade scans projected
 *    battlefield permanents for the keyword whenever the designation changes (CR 702.145b, second
 *    ability); the `DayNightCheck` state-based sweep catches permanents that arrive out of step
 *    (CR 702.145c).
 *  - **"Can't transform except due to its daybound ability"** — enforced at the `TransformEffect`
 *    executor boundary (CR 702.145b, third ability).
 *
 * Controlling a daybound permanent while it's neither day nor night makes it day (CR 702.145d), also
 * handled by `DayNightCheck`. Because every one of these reads projected state, a *granted* daybound
 * works and nothing beyond the keyword tag is needed. Pair with [nightbound] on the back face. See
 * [com.wingedsheep.sdk.core.DayNight].
 */
fun CardBuilder.daybound() {
    keywords(Keyword.DAYBOUND)
}

/**
 * Add **Nightbound** (CR 702.145) to a transforming double-faced card's **back** face — the opposite
 * face of a [daybound] front.
 *
 * The mirror of [daybound], and just as much "only the keyword." Nightbound's two static abilities are
 * read off projected state by the same engine machinery:
 *
 *  - **"As it becomes day, if back face up, transform it"** — the day/night transform cascade flips
 *    back-face-up nightbound permanents to their front when it becomes day (CR 702.145e, first
 *    ability); `DayNightCheck` catches out-of-step arrivals (CR 702.145f).
 *  - **"Can't transform except due to its nightbound ability"** — enforced at the `TransformEffect`
 *    executor boundary (CR 702.145e, second ability).
 *
 * Controlling a nightbound permanent while it's neither day nor night, with no daybound permanent on
 * the battlefield, makes it night (CR 702.145g). A *granted* nightbound works for the same
 * projected-state reason as [daybound]. See [com.wingedsheep.sdk.core.DayNight].
 */
fun CardBuilder.nightbound() {
    keywords(Keyword.NIGHTBOUND)
}

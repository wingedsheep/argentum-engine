package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword

/**
 * Add Decayed (CR 702.147, Innistrad: Midnight Hunt).
 *
 * "This creature can't block, and when it attacks, sacrifice it at end of combat."
 *
 * Only the keyword is authored: the engine realizes both halves off the projected
 * [Keyword.DECAYED] — "can't block" in projection, the attack-triggered end-of-combat
 * self-sacrifice in trigger detection — the same way it does for a token created "with
 * decayed" (`CreateTokenEffect.keywords`), a decayed counter, or a granted Decayed. So a
 * creature that loses all abilities loses both halves with the keyword.
 */
fun CardBuilder.decayed() {
    keywordSet.add(Keyword.DECAYED)
}

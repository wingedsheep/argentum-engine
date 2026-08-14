package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword

/**
 * Add **Storied** (The Hobbit, CR 702.195a).
 *
 * "Any time you control three or more permanents that are artifacts, Sagas, and/or legendary and you
 * don't have an enduring story, you have an enduring story for the rest of the game."
 *
 * Nothing but the keyword is wired here, and that is the whole design — the same one
 * [startYourEngines] uses, for the same reason. Handing out the *enduring story* designation is a
 * state-based action, not a triggered ability, so the engine's `StoriedEnduringStoryCheck` does it by
 * scanning projected battlefield permanents for [Keyword.STORIED]. What falls out for free:
 *
 * - The threshold is re-read continuously, so a storied one-drop that lands on turn one still hands
 *   you the designation on the turn your third qualifying permanent arrives — an enters-the-battlefield
 *   trigger would sample the count once and never look again.
 * - Gaining control of an opponent's storied permanent gives *you* the designation.
 * - Granting storied at runtime works, because the scan reads projected state (Layer 6).
 *
 * The payoff half is an ordinary condition — gate the ability on
 * [Conditions.YouHaveEnduringStory], usually through `staticAbility { condition = … }`:
 *
 * ```kotlin
 * storied()
 * staticAbility {
 *     condition = Conditions.YouHaveEnduringStory
 *     ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source())
 * }
 * ```
 *
 * Per CR 702.195b the designation itself has no rules meaning beyond being a marker, and per
 * 702.195a it is never lost once gained — dropping back below three qualifying permanents, or losing
 * the storied permanent entirely, keeps it.
 */
fun CardBuilder.storied() {
    keywords(Keyword.STORIED)
}

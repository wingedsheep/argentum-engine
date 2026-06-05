package com.wingedsheep.tooling.coverage.bridge

/** Evergreen keywords that map straight to a `Keyword` enum member. Most other keywords resolve for
 *  free via the probe's PascalCase→enum auto-resolve; these are the ones worth pinning explicitly. */
internal fun BridgeBuilder.keywords() {
    keyword("Flying", "FLYING")
    keyword("Haste", "HASTE")
    keyword("Vigilance", "VIGILANCE")
    keyword("Reach", "REACH")
    keyword("Defender", "DEFENDER")

    composed("Landwalk", "specific *WALK keywords (SWAMPWALK, FORESTWALK, ...)")
}

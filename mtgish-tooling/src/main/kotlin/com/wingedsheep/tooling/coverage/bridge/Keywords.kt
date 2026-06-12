package com.wingedsheep.tooling.coverage.bridge

/** Evergreen keywords that map straight to a `Keyword` enum member. Most other keywords resolve for
 *  free via the probe's PascalCase→enum auto-resolve; these are the ones worth pinning explicitly. */
internal fun BridgeBuilder.keywords() {
    keyword("Flying", "FLYING")
    keyword("Haste", "HASTE")
    keyword("Vigilance", "VIGILANCE")
    keyword("Reach", "REACH")
    keyword("Defender", "DEFENDER")
    // Intimidate (CR 702.13) — `Keyword.INTIMIDATE` exists in the SDK enum, so the PascalCase→enum
    // auto-resolve would accept it, but the rules engine has NO block-evasion handling for it
    // (BlockEvasionRules covers flying/fear/shadow/horsemanship/landwalk only). A bare or granted
    // intimidate would compile, lint, and snapshot fine while doing nothing in combat — a silent
    // no-op. Pin it blocking until the engine implements it; then delete this line.
    unsupported("Intimidate", "Keyword.INTIMIDATE is enum-only — no BlockEvasionRules handling; implement intimidate block evasion (CR 702.13) to unlock")
    // Saddle N (CR 702.171) — a PARAMETERIZED keyword ability (the N count rides in the rule's args),
    // NOT a bare card keyword. It must be `supported`, not `keyword`: a `keyword` entry would make
    // `keywordLines` stamp a bare `keywords(Keyword.SADDLE)` on the card and drop the N, exactly the
    // parameterized-keyword trap the `keywordLines` guard warns about (and why Crew carries no bridge
    // entry at all). The emitter's explicit `rname == "Saddle"` branch renders
    // `keywordAbility(KeywordAbility.saddle(N))`; this `supported` entry only marks the capability as
    // covered (never blocking) so the probe doesn't report Saddle as a gap.
    supported("Saddle", "keyword ability: Saddle N -> keywordAbility(KeywordAbility.saddle(N)) (CR 702.171)")
    // Firebending N (CR 702.189, Avatar: The Last Airbender) — a PARAMETERIZED keyword ability (the
    // N count rides in the rule's args as a nested _GameNumber:Integer, exactly like Saddle). Must be
    // `supported`, not `keyword`: a bare `keywords(Keyword.FIREBENDING)` would drop the N. The
    // emitter's `rname == "Firebending"` branch renders `keywordAbility(KeywordAbility.firebending(N))`;
    // the integer case auto-renders, while "firebending X (X = its power)" carries an XValue node and
    // the emitter declines it (-> SCAFFOLD). This entry only marks the capability covered.
    supported("Firebending", "keyword ability: Firebending N -> keywordAbility(KeywordAbility.firebending(N)) (CR 702.189)")

    composed("Landwalk", "specific *WALK keywords (SWAMPWALK, FORESTWALK, ...)")
    // Equip is a keyword ability, but the engine has no `Keyword.EQUIP` enum member: `equipAbility(cost)`
    // synthesises the sorcery-speed "attach to target creature you control" activated ability whose
    // resolution is `AttachEquipment`. So it's composed, not a bare keyword (which would emit a
    // non-existent enum). The emitter renders only the canonical unrestricted shape (see Emitter.equipAbilityLine).
    composed("Equip", "equip: equipAbility(cost) -> sorcery-speed AttachEquipment activated ability", composes = listOf("AttachEquipment"))
    // Cycling (CR 702.29) is a keyword ability with no `Keyword.CYCLING` enum member — `KeywordAbility.cycling(cost)`
    // synthesises the activated "Discard this card: Draw a card" ability. Like Equip it's composed, not a bare
    // keyword (PascalCase auto-resolve would look for a non-existent CYCLING enum and block it). The emitter renders
    // the canonical pure-mana shape (see Emitter.kt `rname == "Cycling"`).
    composed("Cycling", "cycling: KeywordAbility.cycling(cost) -> 'Discard this card: Draw a card' activated ability", composes = listOf("DrawCards"))

    // Station (CR 702.184, Edge of Eternities). Three IR rules. Like Saddle, these are activated/static
    // abilities the engine fully supports but that aren't bare card keywords, so they're `supported`
    // (never blocking) rather than `keyword` (which would stamp a non-existent Keyword.STATION enum).
    //  - `Station` itself: the keyword ability -> the `station()` builder (Emitter `rname == "Station"`).
    //  - `StationChargedAnimate`: the {N+} symbol that turns the permanent into a creature -> threshold-gated
    //    GrantCardType + GrantKeyword rows (Emitter `rname == "StationChargedAnimate"`).
    //  - `StationCharged`: the non-animating {N+} symbol that gates an activated/triggered ability. The engine
    //    expresses it (ActivationRestriction.OnlyIfCondition + Conditions.SourceCounterCountAtLeast), so it's
    //    covered; the emitter declines to auto-render its arbitrary payload and scaffolds (default branch).
    supported("Station", "keyword ability: Station (CR 702.184a) -> station() builder; adds charge counters = tapped creature's power")
    supported("StationChargedAnimate", "{N+} station symbol -> animate into a creature: gated GrantCardType + GrantKeyword rows (CR 721.2b)")
    supported("StationCharged", "{N+} station symbol gating an ability (CR 721.2a) -> OnlyIfCondition(SourceCounterCountAtLeast(CHARGE, N)); emitter scaffolds the payload")
}
